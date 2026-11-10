# Supervision checklist

## Emile does (hands, eyes, accounts)

- [ ] **Approve the order** for 2 × Google TV Streamer (338 €) and, optionally, 2 HDMI cables ([HARDWARE.md](HARDWARE.md)).
- [ ] **Plug in** each box on HDMI 1, with power and Ethernet if possible.
- [ ] **TV settings** with the TV remote ([TV-SETUP.md](TV-SETUP.md)): CEC on, tracking off, TV offline.
- [ ] **Google setup** of each box with the Google Home app: Wi-Fi, account, 2FA.
- [ ] **Install and sign in to** Netflix, YouTube, Prime Video and Canal+ (Canal+: a code at androidtv.canalplus.com). Passwords and 2FA are yours.
- [ ] **Remote setup:** TV power/volume/input for LG and Samsung, ★ = Prime Video, hold YouTube = SmartTube ([REMOTE.md](REMOTE.md)).
- [ ] **Developer mode** on each box: Settings > System > About > click *Android TV OS build* 7×, then Developer options > **USB debugging** on (and **Wireless debugging** on the Streamer). Note the IP (Settings > Network).
- [ ] **Accept "Allow debugging?"** on the TV the first time the script connects (tick *Always allow from this computer*).
- [ ] **Play Store auto-update off** on each box (adb can't do it).
- [ ] **Final test** in each room: TV off > press Netflix > TV wakes on HDMI 1 with Netflix. Home > CapITV. 1–4 open the four apps.

## An agent can do (over adb from your Mac, same Wi-Fi)

- [x] Build and release the CapITV APK (CI does it on every tag).
- [ ] Run `provision/capitv-provision.sh --dry-run <ip>` and show you the plan.
- [ ] Run it for real: install CapITV, make it Home, debloat, Private DNS, update flag.
- [ ] Install SmartTube from its official GitHub release (`--smarttube`).
- [ ] Open the Play Store page of a missing app on the TV (`--open-store`). Tapping *Install* is still yours.
- [ ] Create a NextDNS profile (ads, trackers, Google/LG/Samsung telemetry lists). You approve the account.
- [ ] Undo everything with `--restore` if something misbehaves.
- [ ] A monthly reminder to update the four apps by hand.

## Order of operations, per TV

1. TV settings → 2. plug in the box and run Google setup → 3. Apps-only mode → 4. install the four apps and sign in
→ 5. remote setup → 6. developer mode → 7. `capitv-provision.sh --dry-run <ip>` → 8. `capitv-provision.sh --smarttube --dns <id>.dns.nextdns.io <ip>`
→ 9. final test. Then the second TV, identically.

## If something goes wrong

| Symptom | Fix |
|---|---|
| Home still opens Google TV | Settings > Accessibility > **CapITV Home button** on, or re-run the script |
| TV no longer switches input on button press | Switch off the CapITV Home button service (see REMOTE.md). CapITV stays reachable from the apps row |
| An app breaks after Private DNS | Allowlist its domains in NextDNS, or `--restore` |
| Anything else | `provision/capitv-provision.sh --restore <ip>` puts the box back as it was |
