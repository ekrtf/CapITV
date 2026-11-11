#!/usr/bin/env bash
# CapITV provisioning: turns a certified Google TV / Android TV box into a
# 4-app CapITV box over adb. Idempotent, reversible, bash 3.2+ (macOS) and Linux.
#
#   capitv-provision.sh [options] <box-ip[:port]>
#   capitv-provision.sh --restore [options] <box-ip[:port]>
#
# Run with --help for the options.

set -euo pipefail

VERSION="0.1.0"
REPO="ekrtf/CapITV"
PKG="com.ekrtf.capitv"
HOME_ACTIVITY="$PKG/.MainActivity"
A11Y_SERVICE="$PKG/.HomeKeyService"
GTV_LAUNCHER="com.google.android.apps.tv.launcherx"
GTV_SETUPWRAITH="com.google.android.tungsten.setupwraith"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEBLOAT_FILE="${CAPITV_DEBLOAT_FILE:-$SCRIPT_DIR/debloat.txt}"
LOCAL_APK="$SCRIPT_DIR/../launcher/app/build/outputs/apk/release/app-release.apk"
STATE_ROOT="${CAPITV_STATE_DIR:-${XDG_STATE_HOME:-$HOME/.local/state}/capitv}"

# The four apps, in tile order: "package|label".
APPS="com.netflix.ninja|Netflix
com.google.android.youtube.tv|YouTube
com.amazon.amazonvideo.livingroom|Prime Video
com.canal.android.canal|Canal+"
SMARTTUBE_PKG="org.smarttube.stable"

DRY_RUN=0
RESTORE=0
UNINSTALL=0
AGGRESSIVE=0
HARD_HOME=0
DEBLOAT=1
OPEN_STORE=0
SMARTTUBE=0
DNS_HOST=""
APK=""
TARGET=""
SERIAL=""
STATE_DIR=""
CHANGES=0
TODO_LIST=""

usage() {
  cat <<USAGE
CapITV provisioning $VERSION

Usage: $(basename "$0") [options] <box-ip[:port] | adb-serial>

Setup (default):
  --apk PATH        Install this CapITV APK (default: local build, else the
                    latest GitHub release of $REPO via gh, else curl)
  --dns HOST        Set Private DNS, e.g. abc123.dns.nextdns.io or dns.adguard-dns.com
  --smarttube       Download SmartTube (ad-free YouTube) from its official GitHub
                    releases (yuliskov/SmartTube) and install it
  --open-store      Open the Play Store page of the first missing app on the TV
  --aggressive      Also disable the "optional" packages in debloat.txt
  --hard-home       Google TV only: disable the stock launcher entirely
                    (disables setupwraith first, then launcherx). Breaks casting
                    on the Google TV Streamer. Not needed normally.
  --no-debloat      Skip the debloat step

Undo:
  --restore         Re-enable everything this script disabled, give Home back to
                    the stock launcher, restore the previous Private DNS
  --uninstall       With --restore: also uninstall CapITV

Other:
  --dry-run         Print the commands that would change the box; change nothing
  -h, --help        This help

Before the first run, on the box: Settings > System > About > tap "Android TV OS
build" 7 times, then Settings > System > Developer options > USB debugging ON
(and Wireless debugging ON on Android 14 boxes). See docs/SUPERVISION.md.
USAGE
}

log()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()   { printf '    \033[32mok\033[0m %s\n' "$*"; }
warn() { printf '    \033[33m!!\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[31merror:\033[0m %s\n' "$*" >&2; exit 1; }
todo() { TODO_LIST="${TODO_LIST}  - $*
"; }

# Read-only adb call against the box (also runs in --dry-run).
adbq() { adb -s "$SERIAL" "$@"; }
# Shell output without carriage returns (older adb adds \r).
sh_q() { adbq shell "$@" 2>/dev/null | tr -d '\r'; }
# Mutating adb call: printed in --dry-run, executed otherwise.
# The dry-run line goes to fd 3 (the real stdout, see main) so it still shows when
# the caller sends adb's own output to /dev/null.
adbw() {
  CHANGES=$((CHANGES + 1))
  if [ "$DRY_RUN" -eq 1 ]; then
    {
      printf '    [dry-run] adb -s %s' "$SERIAL"
      printf ' %q' "$@"
      printf '\n'
    } >&3
    return 0
  fi
  adb -s "$SERIAL" "$@"
}

parse_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --apk) [ $# -ge 2 ] || die "--apk needs a path"; APK="$2"; shift ;;
      --dns) [ $# -ge 2 ] || die "--dns needs a hostname"; DNS_HOST="$2"; shift ;;
      --smarttube) SMARTTUBE=1 ;;
      --open-store) OPEN_STORE=1 ;;
      --aggressive) AGGRESSIVE=1 ;;
      --hard-home) HARD_HOME=1 ;;
      --no-debloat) DEBLOAT=0 ;;
      --restore) RESTORE=1 ;;
      --uninstall) UNINSTALL=1 ;;
      --dry-run) DRY_RUN=1 ;;
      -h|--help) usage; exit 0 ;;
      -*) die "unknown option: $1 (see --help)" ;;
      *) [ -z "$TARGET" ] || die "only one box per run"; TARGET="$1" ;;
    esac
    shift
  done
  [ -n "$TARGET" ] || { usage; exit 2; }
  if [ -n "$DNS_HOST" ]; then
    case "$DNS_HOST" in
      *[!A-Za-z0-9.-]*|.*|*.) die "--dns must be a plain hostname, e.g. abc123.dns.nextdns.io" ;;
    esac
  fi
  if [ "$UNINSTALL" -eq 1 ] && [ "$RESTORE" -eq 0 ]; then
    die "--uninstall only makes sense with --restore"
  fi
}

connect() {
  command -v adb >/dev/null 2>&1 || die "adb not found. macOS: brew install android-platform-tools; Debian/Ubuntu: sudo apt install adb"
  local out state i
  if adb devices 2>/dev/null | tr -d '\r' | awk 'NR>1 {print $1}' | grep -qxF "$TARGET"; then
    # Already attached (USB cable, emulator, or an existing adb connection).
    SERIAL="$TARGET"
    out="connected to $SERIAL"
    log "Using attached device $SERIAL"
  else
    case "$TARGET" in
      *:*) SERIAL="$TARGET" ;;
      *) SERIAL="$TARGET:5555" ;;
    esac
    log "Connecting to $SERIAL"
    out="$(adb connect "$SERIAL" 2>&1 | tr -d '\r' || true)"
  fi
  case "$out" in
    *"connected to"*) ;;
    *)
      cat >&2 <<HELP
$out
Could not connect. On the box:
  1. Settings > System > Developer options > USB debugging: ON
  2. Android 14 boxes (Google TV Streamer): also Wireless debugging: ON, then
     "Pair device with pairing code" and run:  adb pair <ip>:<pair-port>
     then re-run this script with the port shown under Wireless debugging:
     $(basename "$0") <ip>:<port>
HELP
      exit 1 ;;
  esac
  for i in $(seq 1 30); do
    state="$(adb -s "$SERIAL" get-state 2>/dev/null | tr -d '\r' || true)"
    [ "$state" = "device" ] && break
    [ "$i" -eq 1 ] && warn "Waiting for you to accept \"Allow debugging?\" on the TV (tick Always allow)..."
    sleep 2
  done
  [ "$state" = "device" ] || die "box not authorized (state: ${state:-none}). Accept the prompt on the TV and re-run."
  local model sdk
  model="$(sh_q getprop ro.product.model)"
  sdk="$(sh_q getprop ro.build.version.sdk)"
  ok "connected: ${model:-unknown model}, Android SDK ${sdk:-?}"
  if [ -n "$sdk" ] && [ "$sdk" -lt 30 ] 2>/dev/null; then
    die "CapITV needs Android 11 (SDK 30) or newer; this box reports SDK $sdk"
  fi
  STATE_DIR="$STATE_ROOT/$(printf '%s' "$SERIAL" | tr -c 'A-Za-z0-9._-' '_')"
  if [ "$DRY_RUN" -eq 0 ]; then mkdir -p "$STATE_DIR"; fi
}

# --- package helpers -------------------------------------------------------
PKGS_ALL=""
PKGS_DISABLED=""
refresh_packages() {
  PKGS_ALL="$(sh_q pm list packages | sed 's/^package://')"
  PKGS_DISABLED="$(sh_q pm list packages -d | sed 's/^package://')"
}
installed() { printf '%s\n' "$PKGS_ALL" | grep -qxF "$1"; }
disabled()  { printf '%s\n' "$PKGS_DISABLED" | grep -qxF "$1"; }
is_google_tv() { installed "$GTV_LAUNCHER"; }

state_add() { # file line: append once
  [ "$DRY_RUN" -eq 1 ] && return 0
  touch "$STATE_DIR/$1"
  grep -qxF "$2" "$STATE_DIR/$1" || printf '%s\n' "$2" >> "$STATE_DIR/$1"
}
state_get() { # key -> value from state.env (empty if unset)
  [ -f "$STATE_DIR/state.env" ] || return 0
  sed -n "s/^$1=//p" "$STATE_DIR/state.env" | head -n 1
}
state_set_once() { # key value: only the first (pre-CapITV) value is kept
  [ "$DRY_RUN" -eq 1 ] && return 0
  if [ ! -f "$STATE_DIR/state.env" ] || ! grep -q "^$1=" "$STATE_DIR/state.env"; then
    printf '%s=%s\n' "$1" "$2" >> "$STATE_DIR/state.env"
  fi
}

disable_pkg() { # pkg reason
  if ! installed "$1"; then return 0; fi
  if disabled "$1"; then ok "$1 already disabled"; return 0; fi
  adbw shell pm disable-user --user 0 "$1" >/dev/null
  state_add disabled.txt "$1"
  ok "disabled $1 ($2)"
}

# --- setup steps -----------------------------------------------------------
fetch_apk() {
  if [ -n "$APK" ]; then
    [ -f "$APK" ] || die "APK not found: $APK"
    return
  fi
  if [ -f "$LOCAL_APK" ]; then APK="$LOCAL_APK"; return; fi
  local tmp
  tmp="$(mktemp -d)"
  if command -v gh >/dev/null 2>&1 && gh release download --repo "$REPO" --pattern 'capitv-*.apk' --dir "$tmp" >/dev/null 2>&1; then
    APK="$(find "$tmp" -name 'capitv-*.apk' | head -n 1)"
  elif curl -fsSL -o "$tmp/capitv.apk" "https://github.com/$REPO/releases/latest/download/capitv.apk"; then
    APK="$tmp/capitv.apk"
  else
    die "no APK: build it (cd launcher && ./gradlew assembleRelease), pass --apk, or log in with gh (private repo)"
  fi
}

install_launcher() {
  log "Installing the CapITV launcher"
  fetch_apk
  ok "APK: $APK"
  if [ "$DRY_RUN" -eq 1 ]; then adbw install -r "$APK"; return; fi
  local out
  out="$(adb -s "$SERIAL" install -r "$APK" 2>&1 | tr -d '\r' || true)"
  case "$out" in
    *Success*) ok "installed" ;;
    *INSTALL_FAILED_UPDATE_INCOMPATIBLE*)
      warn "installed CapITV was signed with another key; reinstalling (only CapITV itself is removed)"
      adbw uninstall "$PKG" >/dev/null
      adbw install "$APK" | tr -d '\r' | grep -q Success || die "install failed"
      ok "reinstalled" ;;
    *) die "install failed: $out" ;;
  esac
  CHANGES=$((CHANGES + 1))
}

install_smarttube() {
  [ "$SMARTTUBE" -eq 1 ] || return 0
  log "SmartTube (ad-free YouTube, from github.com/yuliskov/SmartTube)"
  if installed "$SMARTTUBE_PKG"; then ok "already installed (it updates itself from its settings)"; return; fi
  local abi asset url tmp
  abi="$(sh_q getprop ro.product.cpu.abi)"
  case "$abi" in arm64-v8a|armeabi-v7a|x86) ;; *) abi="universal" ;; esac
  url="$(curl -fsSL https://api.github.com/repos/yuliskov/SmartTube/releases \
    | grep -o '"browser_download_url": *"[^"]*stable[^"]*'"$abi"'\.apk"' \
    | head -n 1 | sed 's/.*"\(https[^"]*\)"/\1/' || true)"
  [ -n "$url" ] || { warn "could not find a SmartTube stable APK for $abi; install it by hand"; todo "Install SmartTube from https://github.com/yuliskov/SmartTube/releases"; return; }
  asset="${url##*/}"
  ok "found $asset"
  if [ "$DRY_RUN" -eq 1 ]; then adbw install "$asset"; return; fi
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/$asset" "$url"
  if adbw install "$tmp/$asset" | tr -d '\r' | grep -q Success; then
    ok "installed $SMARTTUBE_PKG"
  else
    warn "SmartTube install failed"
  fi
}

check_apps() {
  log "Checking the four apps"
  local line pkg label first_missing=""
  while IFS= read -r line; do
    pkg="${line%%|*}"; label="${line#*|}"
    if installed "$pkg"; then
      ok "$label ($pkg)"
    else
      warn "$label is missing ($pkg)"
      todo "Install $label from the Play Store on the box (search \"$label\")"
      [ -n "$first_missing" ] || first_missing="$pkg"
    fi
  done <<APPLIST
$APPS
APPLIST
  if [ -n "$first_missing" ] && [ "$OPEN_STORE" -eq 1 ]; then
    adbw shell am start -a android.intent.action.VIEW -d "market://details?id=$first_missing" >/dev/null
    ok "opened the Play Store page for $first_missing on the TV; re-run with --open-store for the next one"
  fi
}

set_home() {
  log "Making CapITV the Home screen"
  local prev_home
  prev_home="$(sh_q cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1)"
  case "$prev_home" in
    */*) case "$prev_home" in "$PKG"/*|*ResolverActivity*) ;; *) state_set_once home_activity "$prev_home" ;; esac ;;
  esac
  # 1) Real default home. Works on Shield / Android TV; ignored on Google TV,
  #    where the stock launcher wins HOME by priority.
  if [ "$DRY_RUN" -eq 1 ]; then
    adbw shell cmd package set-home-activity --user 0 "$HOME_ACTIVITY"
  elif adb -s "$SERIAL" shell cmd package set-home-activity --user 0 "$HOME_ACTIVITY" 2>&1 | tr -d '\r' | grep -qi success; then
    CHANGES=$((CHANGES + 1))
    ok "set as default home activity"
  else
    warn "set-home-activity not accepted (normal on Google TV)"
  fi

  # 2) HOME key service (needed on Google TV). Keep any services the user already had.
  local current new
  current="$(sh_q settings get secure enabled_accessibility_services)"
  [ "$current" = "null" ] && current=""
  state_set_once a11y_services "${current:-null}"
  state_set_once a11y_enabled "$(sh_q settings get secure accessibility_enabled)"
  case ":$current:" in
    *":$A11Y_SERVICE:"*) ok "Home button service already on" ;;
    *)
      new="${current:+$current:}$A11Y_SERVICE"
      adbw shell settings put secure enabled_accessibility_services "$new"
      adbw shell settings put secure accessibility_enabled 1
      ok "Home button service on (Settings > Accessibility > CapITV Home button)" ;;
  esac

  # 3) Optional: remove the stock Google TV launcher completely.
  if [ "$HARD_HOME" -eq 1 ]; then
    if is_google_tv; then
      warn "--hard-home: casting to this box will stop working; --restore undoes it"
      # Order matters: setupwraith's RecoveryActivity would otherwise grab HOME and
      # crash-loop system_server once launcherx is gone.
      disable_pkg "$GTV_SETUPWRAITH" "setup/recovery HOME handler"
      disable_pkg "$GTV_LAUNCHER" "stock Google TV launcher"
      adbw shell cmd package set-home-activity --user 0 "$HOME_ACTIVITY" >/dev/null || true
    else
      warn "--hard-home is only for Google TV boxes; skipped"
    fi
  fi
}

debloat() {
  [ "$DEBLOAT" -eq 1 ] || { log "Debloat skipped (--no-debloat)"; return; }
  [ -f "$DEBLOAT_FILE" ] || die "missing $DEBLOAT_FILE"
  log "Disabling bloat from $(basename "$DEBLOAT_FILE")$([ "$AGGRESSIVE" -eq 1 ] && printf ' (aggressive)')"
  local line pkg tier reason
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%%$'\r'}"
    case "$line" in ''|'#'*) continue ;; esac
    reason="${line#*#}"; [ "$reason" = "$line" ] && reason=""
    reason="$(printf '%s' "$reason" | sed 's/^ *//')"
    # shellcheck disable=SC2086
    set -- ${line%%#*}
    pkg="${1:-}"; tier="${2:-core}"
    [ -n "$pkg" ] || continue
    # Same list as the NEVER block at the top of debloat.txt.
    case "$pkg" in
      com.google.android.gms|com.android.vending|com.google.android.apps.mediashell|\
      com.android.tv.settings|com.google.android.tv.remote.service|\
      "$PKG"|"$GTV_LAUNCHER"|"$GTV_SETUPWRAITH")
        warn "refusing to disable protected package $pkg (edit debloat.txt)"; continue ;;
    esac
    if [ "$tier" = "optional" ] && [ "$AGGRESSIVE" -eq 0 ]; then continue; fi
    disable_pkg "$pkg" "${reason:-$tier}"
  done < "$DEBLOAT_FILE"
}

set_dns() {
  [ -n "$DNS_HOST" ] || return 0
  log "Private DNS -> $DNS_HOST"
  state_set_once dns_mode "$(sh_q settings get global private_dns_mode)"
  state_set_once dns_specifier "$(sh_q settings get global private_dns_specifier)"
  if [ "$(sh_q settings get global private_dns_mode)" = "hostname" ] && \
     [ "$(sh_q settings get global private_dns_specifier)" = "$DNS_HOST" ]; then
    ok "already set"; return
  fi
  adbw shell settings put global private_dns_specifier "$DNS_HOST"
  adbw shell settings put global private_dns_mode hostname
  ok "set (if streaming apps misbehave, allowlist their domains in your DNS provider)"
}

quiet_updates() {
  log "Updates"
  # Best effort: honoured by some Android TV builds, ignored by others.
  state_set_once ota_disable "$(sh_q settings get global ota_disable_automatic_update)"
  if [ "$(sh_q settings get global ota_disable_automatic_update)" = "1" ]; then
    ok "automatic system update flag already off"
  else
    adbw shell settings put global ota_disable_automatic_update 1
    ok "asked the system not to auto-install OTAs (best effort, not honoured on every box)"
  fi
  todo "Play Store > profile icon > Settings > Auto-update apps: Don't auto-update apps (adb cannot change this)"
  if is_google_tv; then
    todo "Google TV system updates cannot be turned off without root; they install on standby. That is the trade-off of a certified box."
    todo "Settings > Accounts & Profiles > your profile > Apps only mode: ON (removes the stock home's recommendations if you ever see it)"
  fi
  if installed com.nvidia.ota && [ "$AGGRESSIVE" -eq 0 ]; then
    todo "Shield: to stop system updates, re-run with --aggressive (disables com.nvidia.ota)"
  fi
}

verify() {
  log "Verifying"
  [ "$DRY_RUN" -eq 1 ] && { ok "skipped in dry-run"; return; }
  refresh_packages
  installed "$PKG" || die "CapITV is not installed"
  if adb -s "$SERIAL" shell am start -n "$HOME_ACTIVITY" >/dev/null 2>&1; then
    ok "CapITV opened on the TV"
  else
    warn "could not open CapITV"
  fi
  local perms
  perms="$(sh_q dumpsys package "$PKG" | grep -c 'android.permission.INTERNET' || true)"
  if [ "$perms" = "0" ]; then
    ok "CapITV has no INTERNET permission"
  else
    warn "CapITV unexpectedly lists INTERNET"
  fi
}

# --- restore ---------------------------------------------------------------
restore() {
  log "Restoring $SERIAL"
  local pkg list
  if [ -f "$STATE_DIR/disabled.txt" ]; then
    list="$(cat "$STATE_DIR/disabled.txt")"
  else
    warn "no saved state for this box; re-enabling every package in debloat.txt and the stock launcher that is disabled"
    list="$(sed -e 's/#.*//' -e '/^[[:space:]]*$/d' "$DEBLOAT_FILE" | awk '{print $1}')
$GTV_LAUNCHER
$GTV_SETUPWRAITH"
  fi
  # Launcher first, then setupwraith: the reverse of the safe disable order.
  for pkg in $GTV_LAUNCHER $GTV_SETUPWRAITH $list; do
    if installed "$pkg" && disabled "$pkg"; then
      adbw shell pm enable "$pkg" >/dev/null
      PKGS_DISABLED="$(printf '%s\n' "$PKGS_DISABLED" | grep -vxF "$pkg" || true)"
      ok "enabled $pkg"
    fi
  done

  local current cleaned
  current="$(sh_q settings get secure enabled_accessibility_services)"
  cleaned="$(printf '%s' "$current" | tr ':' '\n' | grep -vxF "$A11Y_SERVICE" | grep -v '^null$' | paste -sd: - || true)"
  if [ "$current" != "$cleaned" ]; then
    if [ -n "$cleaned" ]; then
      adbw shell settings put secure enabled_accessibility_services "$cleaned"
    else
      adbw shell settings delete secure enabled_accessibility_services >/dev/null
      [ "$(state_get a11y_enabled)" = "1" ] || adbw shell settings put secure accessibility_enabled 0
    fi
    ok "Home button service off"
  fi
  local prev_home
  prev_home="$(state_get home_activity)"
  if [ -n "$prev_home" ]; then
    adbw shell cmd package set-home-activity --user 0 "$prev_home" >/dev/null 2>&1 || true
    ok "default home set back to $prev_home"
  fi

  local mode spec
  mode="$(state_get dns_mode)"; spec="$(state_get dns_specifier)"
  if [ -n "$mode" ]; then
    if [ "$mode" = "null" ]; then adbw shell settings delete global private_dns_mode >/dev/null; else adbw shell settings put global private_dns_mode "$mode"; fi
    if [ "$spec" = "null" ] || [ -z "$spec" ]; then adbw shell settings delete global private_dns_specifier >/dev/null; else adbw shell settings put global private_dns_specifier "$spec"; fi
    ok "Private DNS restored ($mode)"
  fi
  local ota
  ota="$(state_get ota_disable)"
  if [ -n "$ota" ]; then
    if [ "$ota" = "null" ]; then adbw shell settings delete global ota_disable_automatic_update >/dev/null; else adbw shell settings put global ota_disable_automatic_update "$ota"; fi
    ok "system update setting restored"
  fi

  if [ "$UNINSTALL" -eq 1 ] && installed "$PKG"; then
    adbw uninstall "$PKG" >/dev/null
    ok "CapITV uninstalled"
  fi
  if [ "$DRY_RUN" -eq 0 ] && [ -d "$STATE_DIR" ]; then
    rm -f "$STATE_DIR/disabled.txt" "$STATE_DIR/state.env"
  fi
  todo "Press Home on the remote: the stock home screen should be back"
}

summary() {
  echo
  if [ "$DRY_RUN" -eq 1 ]; then
    log "Dry run: $CHANGES change(s) would be made. Nothing was changed."
  else
    log "Done: $CHANGES change(s) made. State saved in $STATE_DIR"
  fi
  if [ -n "$TODO_LIST" ]; then
    echo "Left for you to do on the TV:"
    printf '%s' "$TODO_LIST"
  fi
}

main() {
  exec 3>&1
  parse_args "$@"
  connect
  refresh_packages
  if [ "$RESTORE" -eq 1 ]; then
    restore
  else
    install_launcher
    install_smarttube
    refresh_packages
    check_apps
    set_home
    debloat
    set_dns
    quiet_updates
    verify
  fi
  summary
}

main "$@"
