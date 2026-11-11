#!/usr/bin/env bash
# Tests capitv-provision.sh against fake-adb (no TV needed). Run: provision/tests/test-provision.sh
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT="$HERE/../capitv-provision.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
PASS=0

mkdir -p "$WORK/bin" "$WORK/adb"
cp "$HERE/fake-adb" "$WORK/bin/adb"
chmod +x "$WORK/bin/adb"
export PATH="$WORK/bin:$PATH" FAKE_ADB_DIR="$WORK/adb" CAPITV_STATE_DIR="$WORK/state"
touch "$WORK/fake.apk"

fail() { echo "FAIL: $*" >&2; echo "--- adb log:" >&2; cat "$WORK/adb/log.txt" >&2; exit 1; }
pass() { PASS=$((PASS + 1)); echo "ok - $*"; }
has()  { grep -qxF "$2" "$WORK/adb/$1"; }
setting() { grep "^$1 $2=" "$WORK/adb/settings.txt" | sed "s/^$1 $2=//"; }

cat > "$WORK/adb/packages.txt" <<PKGS
com.google.android.apps.tv.launcherx
com.google.android.tungsten.setupwraith
com.google.android.gms
com.android.vending
com.google.android.play.games
com.google.android.youtube.tvmusic
com.google.android.feedback
com.google.android.katniss
com.netflix.ninja
com.google.android.youtube.tv
PKGS
echo "secure enabled_accessibility_services=com.example/.Existing" > "$WORK/adb/settings.txt"
echo "global private_dns_mode=opportunistic" >> "$WORK/adb/settings.txt"

run() { bash "$SCRIPT" --apk "$WORK/fake.apk" "$@" 192.168.1.50 > "$WORK/out.txt" 2>&1 || { cat "$WORK/out.txt"; fail "script exited non-zero: $*"; }; }

# 1. dry run changes nothing
run --dry-run --dns abc.dns.nextdns.io
grep -q '\[dry-run\]' "$WORK/out.txt" || fail "dry-run printed no commands"
grep -qF '[dry-run] adb -s 192.168.1.50:5555 shell pm disable-user --user 0 com.google.android.play.games' "$WORK/out.txt" \
  || fail "dry-run hid the debloat commands"
[ ! -s "$WORK/adb/disabled.txt" ] || fail "dry-run disabled packages"
has packages.txt com.ekrtf.capitv && fail "dry-run installed the APK"
[ ! -d "$WORK/state" ] || fail "dry-run wrote state"
pass "dry-run changes nothing"

# 2. real run
run --dns abc.dns.nextdns.io
has packages.txt com.ekrtf.capitv || fail "APK not installed"
has disabled.txt com.google.android.play.games || fail "core bloat not disabled"
has disabled.txt com.google.android.feedback || fail "telemetry not disabled"
has disabled.txt com.google.android.katniss && fail "optional package disabled without --aggressive"
has disabled.txt com.google.android.gms && fail "protected package disabled"
has disabled.txt com.google.android.apps.tv.launcherx && fail "stock launcher disabled without --hard-home"
[ "$(setting secure enabled_accessibility_services)" = "com.example/.Existing:com.ekrtf.capitv/.HomeKeyService" ] || fail "a11y service not appended"
[ "$(setting global private_dns_mode)" = "hostname" ] || fail "DNS mode"
[ "$(setting global private_dns_specifier)" = "abc.dns.nextdns.io" ] || fail "DNS host"
grep -q "Prime Video is missing" "$WORK/out.txt" || fail "missing app not reported"
grep -q "Auto-update apps" "$WORK/out.txt" || fail "manual update step not listed"
pass "setup disables core bloat, keeps protected/optional, sets Home service and DNS"

# 3. idempotent
cp "$WORK/adb/settings.txt" "$WORK/settings.before"
run --dns abc.dns.nextdns.io
grep -q "already disabled" "$WORK/out.txt" || fail "second run re-disabled packages"
grep -q "Home button service already on" "$WORK/out.txt" || fail "second run re-added service"
grep -q "already set" "$WORK/out.txt" || fail "second run reset DNS"
diff -q "$WORK/settings.before" "$WORK/adb/settings.txt" >/dev/null || fail "second run changed settings"
pass "second run is idempotent"

# 4. aggressive + hard home, safe order
: > "$WORK/adb/log.txt"
run --aggressive --hard-home
has disabled.txt com.google.android.katniss || fail "--aggressive did not disable optional"
has disabled.txt com.google.android.apps.tv.launcherx || fail "--hard-home did not disable launcherx"
sw="$(grep -n 'disable-user --user 0 com.google.android.tungsten.setupwraith' "$WORK/adb/log.txt" | cut -d: -f1)"
lx="$(grep -n 'disable-user --user 0 com.google.android.apps.tv.launcherx' "$WORK/adb/log.txt" | cut -d: -f1)"
if [ -z "$sw" ] || [ -z "$lx" ] || [ "$sw" -ge "$lx" ]; then fail "setupwraith must be disabled before launcherx"; fi
pass "--hard-home disables setupwraith before launcherx"

# 5. restore
run --restore --uninstall
[ ! -s "$WORK/adb/disabled.txt" ] || fail "restore left packages disabled: $(cat "$WORK/adb/disabled.txt")"
[ "$(setting secure enabled_accessibility_services)" = "com.example/.Existing" ] || fail "restore did not keep the user's own a11y service"
[ "$(setting global private_dns_mode)" = "opportunistic" ] || fail "DNS mode not restored"
[ -z "$(setting global private_dns_specifier)" ] || fail "DNS host not cleared"
has packages.txt com.ekrtf.capitv && fail "--uninstall did not remove CapITV"
pass "restore re-enables everything and restores settings"

# 6. argument validation
if bash "$SCRIPT" --dns 'bad host;rm' 1.2.3.4 >/dev/null 2>&1; then fail "bad --dns accepted"; fi
if bash "$SCRIPT" --uninstall 1.2.3.4 >/dev/null 2>&1; then fail "--uninstall without --restore accepted"; fi
bash "$SCRIPT" --help | grep -q "Usage:" || fail "--help"
pass "argument validation"

# 7. the NEVER list in debloat.txt is enforced even if someone adds those packages
printf '%s\n' com.android.tv.settings com.google.android.tv.remote.service >> "$WORK/adb/packages.txt"
printf '%s\n' "com.android.tv.settings core" "com.google.android.tv.remote.service core" > "$WORK/debloat.txt"
CAPITV_DEBLOAT_FILE="$WORK/debloat.txt" run
has disabled.txt com.android.tv.settings && fail "Settings disabled"
has disabled.txt com.google.android.tv.remote.service && fail "remote service disabled"
grep -q "refusing to disable protected package com.google.android.tv.remote.service" "$WORK/out.txt" || fail "no refusal message"
pass "protected packages are never disabled"

echo "All $PASS provisioning tests passed."
