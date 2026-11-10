# The remote: one per room, the same in both

**Verdict: use the Google TV Streamer Voice Remote in both rooms.** Put the LG and
Samsung remotes in a drawer and keep them for TV settings only.

## Can the LG Magic Remote / Samsung remote drive the box?

Partly, and not the way you want.

| | LG Magic Remote (SimpLink = HDMI-CEC) | Samsung remote (Anynet+ = HDMI-CEC) |
|---|---|---|
| Arrows, OK, Back, Play/Pause | ✅ reach the box over CEC | ✅ same |
| Number keys, colour keys | ⚠️ depends on the TV model/firmware | ⚠️ same |
| Pointer / Home | ❌ stays with webOS | ❌ stays with Tizen |
| **Netflix / Prime Video buttons** | ❌ open **LG's own** apps and pull the TV off the box's HDMI input | ❌ same |

The TV intercepts its app buttons before anything goes out on CEC, so Android never
sees them and nothing on the box can remap them. CEC keys aren't normal key events
for remapping tools either ([r/ShieldAndroidTV](https://www.reddit.com/r/ShieldAndroidTV/comments/eu2f3a/remapping_a_button_on_tv_remote_for_shield_cec_use/)).
The one hack, the "Refresh Rate" app, is Shield-only and limited.

## What the Streamer remote does

Sources: [Google: Streamer Voice Remote buttons](https://support.google.com/googletv/answer/15323195?hl=en),
[Google: customizable button](https://support.google.com/chromecast/answer/15157668?hl=en),
[Google: TV power/volume/input](https://support.google.com/chromecast/answer/10094150?hl=en).

- **Netflix** and **YouTube** buttons work from anywhere. Hold YouTube to choose which installed YouTube app it opens.
- A **customizable star button**: one app, the Home panel, or input switching. Hold it 1–2 s to change it.
- **TV power, volume, mute and input** over IR or CEC, learned for LG and Samsung during setup.
- Pressing a button wakes the TV and switches it to the box's input (CEC One Touch Play).

## The layout, identical on both TVs

| Button | Opens | How it's set |
|---|---|---|
| **Netflix** | Netflix | built in |
| **YouTube** | SmartTube (or YouTube) | hold YouTube > choose SmartTube. If SmartTube isn't offered, use Button Mapper (below) |
| **★ star** | Prime Video | hold ★ > Prime Video |
| **Hold ★** or **4 / blue / 4th tile** | Canal+ | [Button Mapper](https://buttonmapper.com/remap-android-tv-remote) long-press action, or CapITV |
| **Home** | CapITV | `capitv-provision.sh` (HomeKeyService) |
| Power / Vol / Mute | the TV | Settings > Remotes & Accessories |

Inside CapITV, the same four apps are always on **1 2 3 4** and **red, green, yellow, blue**.
CapITV also handles the Streamer's app-button key codes (`BUTTON_3`, `BUTTON_4`) and the
star button (`MACRO_1`) when they reach it. Normally the system handles them first, which is fine.
All bindings live in [`capitv.json`](../launcher/app/src/main/assets/capitv.json).

### Button Mapper (optional, the same on both boxes)

1. Play Store > **Button Mapper** (by flar2) > install, then grant its accessibility service when it asks.
2. Pick the **★** button > **Long press** > *Launch app* > **Canal+**.
3. If the YouTube long-press menu doesn't offer SmartTube: **YouTube** button > *Single press* > *Launch app* > **SmartTube**.
4. Do exactly the same on the second box.

Button Mapper and CapITV's Home service are both accessibility services. They coexist:
CapITV consumes only the Home key, and the provisioning script keeps services that are already enabled.

## Check this once per TV

Some boxes lose **CEC input switching** when a launcher's accessibility service filters
keys ([Projectivy on the Streamer](https://www.reddit.com/r/Projectivy_Launcher/comments/1qmhovc/google_streamer_projectivy_i_can_either_make_hdmi/)).
CapITV only consumes Home, but test it anyway. With the TV **off**, press **Netflix**: the TV
should wake, switch to HDMI 1 and open Netflix. If it doesn't, switch off
*Settings > Accessibility > CapITV Home button*. Everything else keeps working and
Apps-only mode keeps the stock home clean.

## Why not a 3D-printed or ESP32 remote

It isn't worth it. You'd be rebuilding IR, BLE HID, CEC, battery life and ergonomics
to end up where the bundled remote already is, at 0 €. Only revisit this if you later want one remote for the lights too.
