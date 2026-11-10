---
created: 2026-11-10
tags: [side-project, home, tv, brief]
status: brief
owner: Emile
---

# TV box: one remote, one OS

## Goal and constraints
- **Living room:** LG TV (webOS). **Bedroom:** Samsung TV (Tizen). Great panels, bad software.
- **Apps needed, nothing else:** YouTube, Prime Video, Netflix, Canal+.
- **Wants:** privacy, no ads, no bloat, no constant updates.
- **One remote per TV, the same model in both rooms:** press Netflix and Netflix opens. No second remote, no HDMI source switching.
- **Same box, same apps and same layout on both TVs**, so muscle memory carries over.
- **Each TV becomes a dumb screen:** offline, smart features and tracking off.
- Emile supervises. An agent can run most of the research, ordering and config.

## Reality check: why a Raspberry Pi alone doesn't work
Netflix, Prime Video, Canal+ and (for paid content) YouTube use DRM: Widevine on Android and Linux, FairPlay on Apple. They save HD, 4K, HDR and Dolby for **certified devices with hardware DRM (Widevine L1)**. A Raspberry Pi only ever gets **software Widevine L3**, whether it runs Raspberry Pi OS, LibreELEC/Kodi, or an uncertified Android build like LineageOS/KonstaKANG.
- **Netflix on a Pi:** you have to use a browser. It tops out at 1080p, with no 4K or HDR, and Netflix "can't guarantee it works on Linux" ([raspberry.tips, Oct 2026](https://raspberry.tips/en/raspi-media-center/raspberry-pi-smart-tv-netflix-streaming-live-tv), [RPi forum](https://forums.raspberrypi.com/viewtopic.php?t=370947), [OSFY, Jul 2026](https://www.opensourceforu.com/2026/07/google-ends-widevine-workarounds-on-arm64-linux/)).
- **Prime Video on Linux:** users report SD only in a browser ([raspberry.tips](https://raspberry.tips/en/raspi-media-center/raspberry-pi-smart-tv-netflix-streaming-live-tv)).
- **Kodi add-ons for Netflix/Prime:** unofficial and fragile, and the Netflix one is no longer developed. Team Kodi itself points people to certified boxes like the Shield ([LibreELEC forum](https://forum.libreelec.tv/thread/27726-hardware-suggestions-to-run-le-with-netflix/)).
- **Canal+:** officially supports only browsers on Windows and macOS, plus certified Android TV, Apple TV, Fire TV, consoles and smart TVs. It also warns that rooted or modified devices may get limited content ([Canal+ requirements](https://canal-assistance.canalplus.com/fr/articles/utilisation/quelles-sont-les-conditions-techniques-daccs-canal/66d03d6643ac055ee49e12cb)). On a Pi, expect it to fail or break often.
- **Remote:** a Pi works with the TV remote over CEC for basic navigation, but there's no clean "Netflix button opens Netflix".

**Conclusion:** the "own OS" part has to be a **certified box that you lock down**, not a Pi. A Pi still has a job: optionally running Pi-hole for the network.

## Options compared

| | A. DIY Raspberry Pi 5 (Kodi / Linux / LineageOS) | B. Google TV Streamer, debloated | B'. Nvidia Shield TV Pro, debloated | C. Apple TV 4K (2026) |
|---|---|---|---|---|
| Picture | 1080p max for Netflix, SD for Prime, no HDR or Dolby | 4K, Dolby Vision, HDR10+, Atmos | 4K HDR, Dolby Vision (2019 hardware) | 4K, Dolby Vision, HDR10+, Atmos |
| Apps (YT / Prime / Netflix / Canal+) | Fragile, Canal+ unlikely | All 4 official ([Canal+ on Android TV](https://canal-assistance.canalplus.com/fr/articles/android/accder-canal-sur-android-tv/66d03dab4c79926d91672d03)) | All 4 official | All 4 official ([Canal+](https://boutique.canalplus.com/experience-canal-plus/materiels/ecrans-connectes)) |
| Ads / privacy | Best in theory | Home ads cut by **Apps-only mode**. YouTube ads can go with **SmartTube**. Google account required | Same as B, older OS | No home-screen ads, strong privacy. **YouTube ads stay** unless you pay for Premium |
| Remote | TV remote over CEC, no app buttons | Remote with **Netflix + YouTube + 1 custom button**, IR + CEC for TV power and volume | Shield remote, IR + CEC, programmable button | Siri Remote, CEC for TV power and volume, no app buttons |
| Effort | High, and never really finished | Low to medium (one afternoon per TV) | Low to medium | Lowest |
| Cost for 2 TVs | ~2 × 120 € with case, PSU and card, plus your time | 2 × 169 € = **338 €** | 2 × ~300–350 € | 2 × 349 € = **698 €** |

Prices: [Google Store FR](https://store.google.com/fr/product/google_tv_streamer?hl=fr) (169 €), [Apple FR](https://www.apple.com/fr/shop/buy-tv/apple-tv-4k) (349 €, ships 13/11/2026, [Selectra](https://selectra.info/telecom/actualites/marche/apple-tv-4k-nouvelle-13-octobre-prix-france)), [LDLC Shield Pro](https://www.ldlc.com/en/product/PB00280102.html) (~350 €; it's 2019 hardware and pricier than ever, see [Ecran Large](https://www.ecranlarge.com/high-tech/actu/nvidia-shield-tv-pro-hausse-prix)). The Onn 4K Pro is sold in the US only (Walmart) and Homatics is a niche import. Skip both.

## Recommendation
**Two Google TV Streamers (4K), set up the same way.** Use Apps-only mode, a 4-app home row, SmartTube for ad-free YouTube, NextDNS for ads and telemetry, and the Streamer remote as the only remote in each room. Both TVs go offline and become dumb screens. You get all four apps officially in 4K HDR, one-press app buttons, and the same box and remote in both rooms, for about 340 €.
**Fallback:** if you'd rather pay for YouTube Premium and want zero tinkering, take **2 × Apple TV 4K** (698 €). It's cleaner and more private, but has no app buttons and costs twice as much.

## Remote plan
**Can the LG Magic Remote keep working? Partly, and not the way you want.**
- With **SimpLink (HDMI-CEC)** on, the Magic Remote's arrows, OK, back and play/pause reach the box.
- The **pointer, the Home button and the Netflix/Prime Video hotkeys stay with webOS**. They open LG's own apps and pull you off the box's HDMI input.
- Android can't see those keys to remap them. The TV intercepts them, and CEC keys aren't normal key events for Button Mapper ([r/ShieldAndroidTV](https://www.reddit.com/r/ShieldAndroidTV/comments/eu2f3a/remapping_a_button_on_tv_remote_for_shield_cec_use/)). The one hack, the "Refresh Rate" app, is Shield-only and limited.
- Samsung's Anynet+ remote behaves the same way.
- **Verdict:** use the Google TV Streamer Voice Remote in both rooms. Put the LG and Samsung remotes in a drawer and keep them for TV settings.

**What the Streamer remote does** ([Google: buttons](https://support.google.com/googletv/answer/15323195?hl=en), [Google: TV control](https://support.google.com/chromecast/answer/10094150?hl=en)):
- **Netflix** and **YouTube** buttons. Long-press YouTube to point it at SmartTube.
- A **custom button**, set to Prime Video (or Canal+).
- **Power, volume and mute** on the TV through IR or CEC. It's learned for LG and Samsung during setup.
- Any button press wakes the TV and **switches it to the box's input automatically** (CEC One Touch Play).
- **Button Mapper** can add more shortcuts, for example double-press or long-press for Canal+ ([Button Mapper](https://buttonmapper.com/remap-android-tv-remote)). The Streamer allows remapping even the Netflix button.

**Button layout, identical on both TVs:**
| Button | Action |
|---|---|
| Netflix | Netflix |
| YouTube | SmartTube (or YouTube) |
| Custom (star) | Prime Video |
| Long-press Custom (via Button Mapper) or 4th tile on home | Canal+ |

**3D-printed or ESP32 remote:** not worth it. You'd rebuild IR, BLE HID and CEC, battery life and ergonomics to end up where the 0 € bundled remote already is. Only revisit this if you later want a single remote for lights as well.

## Shopping list (both TVs)
| # | Item | Why | Approx. price | Link |
|---|---|---|---|---|
| 2 | Google TV Streamer (4K), includes Voice Remote | The box plus the one remote. Built-in Gigabit Ethernet, so no adapter needed | 169 € each, **338 €** | [Google Store FR](https://store.google.com/fr/product/google_tv_streamer?hl=fr) |
| 2 | HDMI cable, certified Ultra High Speed, 2 m | Reliable 4K HDR and Dolby Vision. Skip if your current cables are good | ~25–28 € each | [LDLC StarTech 2 m](https://www.ldlc.com/fiche/PB00461655.html) |
| 0–2 | RJ45 Ethernet cable (optional) | More stable than Wi-Fi if a port is nearby | ~5–10 € each | any store |
| 0–1 | NextDNS (optional, recommended) | DNS-level ad and telemetry blocking, configured on the box only. Nothing to host | free tier, or ~2 €/month Pro | [nextdns.io](https://nextdns.io) |
| 0–1 | OR Raspberry Pi Zero 2 W kit + [Pi-hole](https://pi-hole.net) (optional) | Same blocking for the whole home network, hosted at home | ~55 € | [Kubii kit Pi Zero 2 W](https://www.kubii.com/fr/kits-nano-ordinateurs/3477-kits-raspberry-pi-zero-2-w-3272496308275.html) |

**Total:** ~340 € (boxes only) to ~400 € (with cables and Pi-hole).
**Fallback total:** 2 × [Apple TV 4K](https://www.apple.com/fr/shop/buy-tv/apple-tv-4k) at 698 €, plus cables.

## Setup checklist (repeat identically for each TV)
**1. Physical**
- [ ] Plug the Streamer into HDMI 1, the same port on both TVs. Use Ethernet if possible.
- [ ] Unplug Ethernet from the TV itself and forget its Wi-Fi network.

**2. TV settings: LG (webOS)**
- [ ] Settings > General > Devices > HDMI Settings > **SimpLink (HDMI-CEC) On**, and **Auto Power Sync On**.
- [ ] Settings > General > System > Additional Settings > **Live Plus Off**.
- [ ] Settings > Support > Privacy & Terms > User Agreements: **withdraw Viewing Information, Voice Information and interest-based ads** ([guide](https://turnofftracking.com/lg-tv-tracking/)).
- [ ] Turn off **Home Auto Launch** and **Auto Update**, so the TV starts on the last input (the box).

**3. TV settings: Samsung (Tizen)**
- [ ] Settings > Connection > External Device Manager > **Anynet+ (HDMI-CEC) On**.
- [ ] Settings > General & Privacy > Terms & Privacy > **Viewing Information Services Off**, and **Interest-Based Ads Off** ([guide](https://privacygear.nl/en/guides/smart-tv-privacy-acr-disable/)).
- [ ] Smart Features: turn off **Autorun Smart Hub** and **Autorun Last App**. Turn off **Auto Update**.

**4. Box setup**
- [ ] Set up with a Google account. Consider a dedicated "TV" account so viewing history stays off the main account.
- [ ] Settings > Accounts & Profiles > your profile > **Apps-only mode On** ([Android Police](https://www.androidpolice.com/how-to-enable-apps-only-mode-google-tv-streamer/)).
- [ ] Install Netflix, Prime Video, Canal+ and YouTube. Sign in to Canal+ with a code at androidtv.canalplus.com.
- [ ] Uninstall or disable everything else possible: Play Movies, games, News and so on.
- [ ] Home row order: **Netflix, YouTube/SmartTube, Prime Video, Canal+.** Same order on both TVs.
- [ ] Settings > Privacy: turn off usage and diagnostics, ad personalization (delete the ad ID) and location.
- [ ] Play Store > Settings > **Auto-update apps Off**. Update manually about once a month, since old streaming apps eventually break. System updates can't be fully disabled; that's a trade-off.

**5. Remote**
- [ ] Settings > Remotes & Accessories > Set up remote buttons: **Power, Volume and Input set to the TV** (LG or Samsung).
- [ ] Map the custom button to Prime Video. Long-press YouTube and pick SmartTube.
- [ ] Optional: install **Button Mapper** from the Play Store and add the Canal+ shortcut. Do the same on both boxes.
- [ ] Settings > HDMI-CEC on the box: **On** (path varies by firmware).
- [ ] Test with the TV off: press Netflix. The TV should wake, switch to the box and open Netflix.

**6. Ad-free YouTube (optional)**
- [ ] Sideload **SmartTube** from the official source only ([GitHub](https://github.com/yuliskov/SmartTube)), using the Downloader app or ADB. It includes SponsorBlock.
- [ ] Keep the official YouTube app as a fallback.

**7. DNS blocking (optional)**
- [ ] Create a NextDNS profile with ads, trackers and Google/Samsung/LG telemetry blocklists. Point the box at it through Private DNS. That setting is often hidden on Google TV, so set it via ADB: `adb shell settings put global private_dns_mode hostname` and `adb shell settings put global private_dns_specifier <id>.dns.nextdns.io`.
- [ ] Or set up Pi-hole on the Pi Zero 2 W and set it as DNS on the router.
- [ ] Netflix, Prime and Canal+ sometimes break with aggressive lists. Allowlist when needed.

**8. Optional: custom launcher**
- [ ] Projectivy Launcher removes the last carousel, but on a Google TV Streamer it can **break CEC input switching** unless you turn off its "remap buttons" option ([r/Projectivy_Launcher](https://www.reddit.com/r/Projectivy_Launcher/comments/1qmhovc/google_streamer_projectivy_i_can_either_make_hdmi/)). Try Apps-only mode first, and add Projectivy only if the leftover carousel annoys you.

## Who does what
| Agent can do | Emile must do |
|---|---|
| Check stock and prices, and place the order (after your OK) | Plug in boxes and cables. Change TV settings with the TV remote |
| Create the NextDNS profile and blocklists, and write the Pi-hole config | Google, Netflix, Prime and Canal+ sign-ins, including codes and 2FA |
| Prepare the ADB commands and run them from your Mac if it's on the same Wi-Fi (you enable Developer options and Wireless debugging on the box first) | Pair the remotes and confirm the IR/CEC tests |
| Keep this checklist up to date and give a monthly "update apps" reminder | Final test in each room |

## Open questions
1. Exact LG and Samsung model years? CEC quirks vary, especially on older Samsungs.
2. Any soundbar or AV receiver? It changes who the remote should control for volume.
3. YouTube Premium: yes or no? If yes, SmartTube isn't needed, and Apple TV becomes more attractive.
4. Canal+: app-only subscription, or through an ISP box? With an operator box, the Android TV app may need a commitment-based offer ([Canal+](https://canal-assistance.canalplus.com/fr/articles/android/accder-canal-sur-android-tv/66d03dab4c79926d91672d03)).
5. OK with a Google account on the TVs (a dedicated one), or is that a deal-breaker? If it is, go with Apple TV.
6. Is Ethernet reachable behind both TVs?

## Sources
- [raspberry.tips: Raspberry Pi as a smart TV, Netflix/Prime limits (Oct 2026)](https://raspberry.tips/en/raspi-media-center/raspberry-pi-smart-tv-netflix-streaming-live-tv)
- [Raspberry Pi forum: Netflix 1080p max on Pi](https://forums.raspberrypi.com/viewtopic.php?t=370947)
- [OSFY: Widevine on Arm64 Linux (Jul 2026)](https://www.opensourceforu.com/2026/07/google-ends-widevine-workarounds-on-arm64-linux/)
- [LibreELEC forum: Netflix needs a certified device](https://forum.libreelec.tv/thread/27726-hardware-suggestions-to-run-le-with-netflix/)
- [Canal+: technical requirements](https://canal-assistance.canalplus.com/fr/articles/utilisation/quelles-sont-les-conditions-techniques-daccs-canal/66d03d6643ac055ee49e12cb) · [Canal+ on Android TV](https://canal-assistance.canalplus.com/fr/articles/android/accder-canal-sur-android-tv/66d03dab4c79926d91672d03) · [Canal+ compatible devices](https://boutique.canalplus.com/experience-canal-plus/materiels/ecrans-connectes)
- [Google: Streamer Voice Remote buttons](https://support.google.com/googletv/answer/15323195?hl=en) · [Google: TV power/volume/input setup](https://support.google.com/chromecast/answer/10094150?hl=en)
- [Android Police: Apps-only mode](https://www.androidpolice.com/how-to-enable-apps-only-mode-google-tv-streamer/)
- [Button Mapper on Android TV](https://buttonmapper.com/remap-android-tv-remote) · [Reddit: TV-remote CEC keys can't be remapped](https://www.reddit.com/r/ShieldAndroidTV/comments/eu2f3a/remapping_a_button_on_tv_remote_for_shield_cec_use/) · [Reddit: Projectivy vs CEC on Streamer](https://www.reddit.com/r/Projectivy_Launcher/comments/1qmhovc/google_streamer_projectivy_i_can_either_make_hdmi/)
- [SmartTube (GitHub)](https://github.com/yuliskov/SmartTube)
- [Apple: CEC / TV control with Apple TV](https://support.apple.com/en-us/108789) · [Apple TV 4K FR price](https://www.apple.com/fr/shop/buy-tv/apple-tv-4k) · [Selectra: new Apple TV 4K, 349 €](https://selectra.info/telecom/actualites/marche/apple-tv-4k-nouvelle-13-octobre-prix-france)
- [Google TV Streamer FR (169 €)](https://store.google.com/fr/product/google_tv_streamer?hl=fr) · [LDLC Shield TV Pro](https://www.ldlc.com/en/product/PB00280102.html) · [Ecran Large: Shield price in 2026](https://www.ecranlarge.com/high-tech/actu/nvidia-shield-tv-pro-hausse-prix)
- [LG tracking off guide](https://turnofftracking.com/lg-tv-tracking/) · [Smart TV ACR off (LG/Samsung)](https://privacygear.nl/en/guides/smart-tv-privacy-acr-disable/)
- [NextDNS](https://nextdns.io) · [Pi-hole](https://pi-hole.net) · [Kubii Pi Zero 2 W kit](https://www.kubii.com/fr/kits-nano-ordinateurs/3477-kits-raspberry-pi-zero-2-w-3272496308275.html)
