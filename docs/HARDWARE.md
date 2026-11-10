# Hardware

Prices were checked on 10 Nov 2026 (France, incl. VAT) and links were checked live. Prices move, so re-check before ordering.

## Shopping list (both TVs)

| Qty | Item | Why | Approx. price | Link |
|---|---|---|---|---|
| 2 | **Google TV Streamer (4K)**, Voice Remote included | The box and the only remote. Widevine L1, 4K, Dolby Vision, HDR10+, Atmos, Gigabit Ethernet built in, Android 14 | 169 € each, **338 €** | [Google Store FR](https://store.google.com/fr/product/google_tv_streamer?hl=fr) |
| 2 | HDMI cable, certified Ultra High Speed, 2 m | Google asks for HDMI 2.1 (not included). Reliable 4K HDR / Dolby Vision. Skip if you already have good ones | ~25–28 € each | [LDLC, StarTech 2 m](https://www.ldlc.com/fiche/PB00461655.html) |
| 0–2 | RJ45 Ethernet cable | More stable than Wi-Fi if a port is near the TV | ~5–10 € each | any store |
| 0–1 | **NextDNS** (recommended) | DNS-level ad and telemetry blocking, set on the box by the script (`--dns`). Nothing to host | free tier, or ~2 €/month | [nextdns.io](https://nextdns.io) |
| 0–1 | *or* Raspberry Pi Zero 2 W kit + [Pi-hole](https://pi-hole.net) | The same blocking for the whole home network, hosted at home | ~55 € | [Kubii kit Pi Zero 2 W](https://www.kubii.com/fr/kits-nano-ordinateurs/3477-kits-raspberry-pi-zero-2-w-3272496308275.html) |

**Total:** ~340 € (boxes only) to ~400 € (with cables and Pi-hole).

Note: the Google Store FR showed 119 € until autumn 2026 (see [Frandroid, Sep 2026](https://www.frandroid.com/produits-android/maison-connectee/3228457_google-abandonne-le-materiel-pas-cher-et-ca-se-voit-sur-les-prix)), and it showed **169 €** on 10 Nov 2026.

## Alternatives the project also supports

| Box | Verdict | Price |
|---|---|---|
| **Nvidia Shield TV Pro** | Works (CapITV supports Android 11, which is what the Shield runs). But it's 2019 hardware, has no AV1 or HDR10+, and costs more than ever. CapITV becomes the real default home there. | ~209–350 € ([Amazon.fr](https://www.amazon.fr/dp/B07Z6RD4M9), [Ecran Large on the 2026 price jump](https://www.ecranlarge.com/high-tech/actu/nvidia-shield-tv-pro-hausse-prix)) |
| Chromecast with Google TV (4K) | Works, but it's discontinued and slower | 69,99 € if you still find one |
| Onn 4K Pro (Walmart) | Works in theory, but it's **US only**. Skip it | n/a |
| Any Android TV 11+ box | Only if it's **Google-certified with Widevine L1**. Cheap AliExpress boxes are usually L3, which means SD Netflix | n/a |

### Documented fallback: Apple TV 4K

If you'd rather pay for YouTube Premium and do zero tinkering, buy **2 × Apple TV 4K**
(349 € each, [Apple FR](https://www.apple.com/fr/shop/buy-tv/apple-tv-4k), [Selectra](https://selectra.info/telecom/actualites/marche/apple-tv-4k-nouvelle-13-octobre-prix-france)).
It's cleaner and more private, with no home-screen ads, and all four apps are official
([Canal+ devices](https://boutique.canalplus.com/experience-canal-plus/materiels/ecrans-connectes)).
But YouTube ads stay without Premium, the Siri Remote has no app buttons, CapITV can't run on it,
and it costs twice as much (698 €).

## Why not a Raspberry Pi

Netflix, Prime Video, Canal+ and paid YouTube content use DRM (Widevine on Android
and Linux). They give HD, 4K, HDR and Dolby only to **certified devices with
hardware-backed Widevine L1**. A Raspberry Pi only ever gets **software Widevine L3**,
whether it runs Raspberry Pi OS, LibreELEC/Kodi or an uncertified Android build such as LineageOS or KonstaKANG.

- **Netflix on a Pi:** browser only, 1080p max, no 4K or HDR, and Netflix "can't guarantee it works on Linux"
  ([raspberry.tips, Oct 2026](https://raspberry.tips/en/raspi-media-center/raspberry-pi-smart-tv-netflix-streaming-live-tv),
  [Raspberry Pi forum](https://forums.raspberrypi.com/viewtopic.php?t=370947),
  [OSFY, Jul 2026: Google ends Widevine workarounds on Arm64 Linux](https://www.opensourceforu.com/2026/07/google-ends-widevine-workarounds-on-arm64-linux/)).
- **Prime Video on Linux:** SD only in a browser ([raspberry.tips](https://raspberry.tips/en/raspi-media-center/raspberry-pi-smart-tv-netflix-streaming-live-tv)).
- **Kodi add-ons for Netflix and Prime:** unofficial and fragile, and Team Kodi points people to certified boxes
  ([LibreELEC forum](https://forum.libreelec.tv/thread/27726-hardware-suggestions-to-run-le-with-netflix/)).
- **Canal+:** supports only Windows and macOS browsers plus certified Android TV, Apple TV, Fire TV,
  consoles and smart TVs. It warns that rooted or modified devices may get limited content
  ([Canal+ requirements](https://canal-assistance.canalplus.com/fr/articles/utilisation/quelles-sont-les-conditions-techniques-daccs-canal/66d03d6643ac055ee49e12cb)).
  CapITV doesn't root or modify the box: it's a normal app plus reversible `pm disable-user` calls, so Widevine L1 stays intact.
- **Remote:** a Pi gets basic navigation over CEC, but there's no "press Netflix, Netflix opens".

The Pi still has a job if you want one: running **Pi-hole** for the whole network.
