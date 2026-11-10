# TV setup: make each TV a dumb screen

Do this once per TV, with the TV's own remote. Menu names vary a little by model year.
Sources: [LG tracking off](https://turnofftracking.com/lg-tv-tracking/), [LG/Samsung ACR off](https://privacygear.nl/en/guides/smart-tv-privacy-acr-disable/).

## Both TVs

- [ ] Streamer on **HDMI 1** in both rooms, so muscle memory carries over.
- [ ] **Keep the TV offline:** unplug its Ethernet and forget its Wi-Fi. The box does all the streaming.
  The TV then can't update, track or show ads. If you ever need a firmware fix,
  reconnect it once, update, and disconnect again.

## LG (webOS), living room

- [ ] Settings > General > Devices > HDMI Settings > **SimpLink (HDMI-CEC): On**, **Auto Power Sync: On**.
- [ ] Settings > General > System > Additional Settings > **Live Plus: Off** (that's LG's ACR, which reads what's on screen).
- [ ] Settings > Support > Privacy & Terms > User Agreements: **withdraw Viewing Information, Voice Information and interest-based ads**.
- [ ] **Home Auto Launch: Off**, so the TV starts on the last input (the box).
- [ ] Settings > Support > Software Update > **Auto Update: Off**.

## Samsung (Tizen), bedroom

- [ ] Settings > Connection > External Device Manager > **Anynet+ (HDMI-CEC): On**.
- [ ] Settings > General & Privacy > Terms & Privacy > **Viewing Information Services: Off**, **Interest-Based Ads: Off**, voice recognition off.
- [ ] Smart Features: **Autorun Smart Hub: Off**, **Autorun Last App: Off**.
- [ ] Software Update > **Auto Update: Off**.

## The box (identical on both)

- [ ] Set it up with a Google account. A dedicated "TV" account keeps viewing history off your main one.
- [ ] Settings > Accounts & Profiles > your profile > **Apps only mode: On** ([Android Police](https://www.androidpolice.com/how-to-enable-apps-only-mode-google-tv-streamer/)).
- [ ] Settings > Privacy: usage & diagnostics **off**, ads > **delete advertising ID**, location **off**.
- [ ] Play Store > profile > Settings > **Auto-update apps: Don't auto-update**. Update by hand
  about once a month, since streaming apps eventually refuse very old versions.
  adb can't change this setting, so the script lists it for you.
- [ ] System updates: these **can't be turned off on Google TV without root** (they install
  on standby). The script sets the best-effort flag `ota_disable_automatic_update=1`. On a
  Shield, `--aggressive` disables `com.nvidia.ota`. That's the trade-off of a certified box.
- [ ] Settings > System > Display & Sound > **HDMI-CEC: On** (path varies by firmware).
