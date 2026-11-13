# CarHub — setup on a low-end stereo

CarHub is a fork of Open Headunit 4.0.0 (AGPL-3.0). It does everything Open Headunit does
(USB and wireless Android Auto, Native AA where the stereo hosts the Wi-Fi, Bluetooth handshake,
steering keys, GPS, mic, overlays), plus:

- **Low-end stereo mode**: on 2 GB RAM / slow-CPU units, music is never dropped to keep pace
  with the screen. This is the usual cause of Spotify stutter on a good network.
  The first start also picks AAC audio, 480p and 30 fps, unless you already set those yourself.
- **AirPlay for iPhone (built in)**: the iPhone's music and mirrored screen play on the stereo
  over the stereo's Wi-Fi hotspot. No cable, no dongle, no extra app, no Bluetooth data channel.
- Its own app id (`com.carhub.autolink`) so it installs next to your current Open Headunit.

## About iPhone: AirPlay vs CarPlay
Wireless **CarPlay cannot work on this stereo** — it must begin with a Bluetooth exchange with the
iPhone, and this stereo's Bluetooth is the maker's own chip that apps cannot send data through.
That is what DiPlay's "nothing reaches the iPhone" message was telling you; it is a hardware limit,
not a setting. **AirPlay** sidesteps it entirely by running over Wi-Fi, so that is what CarHub uses
for iPhones. You get the iPhone's music and full screen mirroring (maps, etc.) on the stereo.

## 1. Install
1. Copy the `.apk` to a USB stick or SD card, open it on the stereo with the file manager and install it.
   (Allow "Install unknown apps" for the file manager if it asks.)
2. Once CarHub works, uninstall or disable Open Headunit / ZLink / AutoKit so they do not fight
   over USB, Bluetooth and Wi-Fi. On 2 GB RAM, every extra app running in the background costs audio stability.

## iPhone (AirPlay) — how to connect
1. Turn on the **stereo's Wi-Fi hotspot** (Native AA already does this; otherwise turn it on in the stereo).
2. On the iPhone, join that hotspot in Wi-Fi settings.
3. Start it one of two ways:
   - Tap the **"CarHub iPhone"** icon in the stereo's app list (manual, do it whenever you want), or
   - Just connect the iPhone to the stereo over Bluetooth — CarHub opens the AirPlay screen by itself.
4. On the iPhone: for music, tap the AirPlay icon and pick **CarHub**; for the whole screen,
   Control Centre → Screen Mirroring → **CarHub**.
- To use DiPlay/CarPlay instead on a stereo that supports it, set Settings → Audio →
  "AirPlay for iPhone" target to the DiPlay app. The "CarHub iPhone" icon then opens DiPlay.

## 2. Android phone, wireless (the stereo makes the hotspot)
Settings → Connection → Wireless mode: **Native AA** (the default).
- The stereo opens the Wi-Fi network; the phone only needs Bluetooth pairing with the stereo once.
- Wi-Fi band: leave on **Auto**. On a 2.4 GHz-only stereo, Auto already lowers the video
  ("Lower video on a 2.4 GHz link") and switches music to AAC.

## 3. Things that still cause Spotify lag (check these on your car)
1. **Phone also connected to the stereo for Bluetooth music (A2DP).** On cheap units, 2.4 GHz Wi-Fi
   and Bluetooth share one antenna. While Android Auto runs, keep Bluetooth for calls only:
   on the phone, open Bluetooth → the stereo → turn off "Media audio".
2. **Phone battery saver** on Spotify / Android Auto / Google Play services. Set them to "Unrestricted".
3. **Spotify streaming quality "Very high" on mobile data.** Use "Automatic" or download playlists.
4. **Software video decoding.** Leave the video decoder on hardware unless the setup wizard says otherwise.
5. In Settings → Audio, keep **Low-end stereo mode** on and **Audio latency** at 16x (deepest).

## 4. If it still stutters, send a log
Settings → Logs → **Start Capturing Logs**, play Spotify for 2 minutes, then **Export Logs**.
Look for lines starting `AudioDecoder.start:` (should say `lowEnd=true, queueCapacity=0`) and any
`gap` / `underrun` lines.

## Building
Needs JDK 21 and the Android SDK (platform 36, build-tools, NDK 29.0.14206865, CMake 3.22.1).

    gradlew assembleGithubRelease      # or assembleGithubDebug

CarHub's source code must stay public under AGPL-3.0 if you share the app.
