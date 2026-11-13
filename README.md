# CarHub

Android Auto **and** AirPlay for an old car stereo, in one app. A fork of
[Open Headunit](https://github.com/andreknieriem/open-headunit) (AGPL-3.0) with a built-in
AirPlay receiver from [android-airplay-server](https://github.com/jqssun/android-airplay-server)
(GPL-3.0, based on [UxPlay](https://github.com/FDH2/UxPlay)).

App id `com.carhub.autolink`, so it installs **next to** your existing Open Headunit.

## Download (install on the stereo)

Open this on the stereo's browser and tap to download, then open the file to install
(allow "install unknown apps" if asked):

- **Most stereos — use this:** [CarHub-4.0.0-lite-32bit.apk](apk/CarHub-4.0.0-lite-32bit.apk?raw=1) (19 MB, 32-bit ARM)
- 64-bit stereos: [CarHub-4.0.0-arm.apk](apk/CarHub-4.0.0-arm.apk?raw=1) (26 MB)

Needs Android 5.0 or newer.

## What it does

- **Android Auto** — wired USB and wireless (Native AA, where the stereo makes the Wi-Fi hotspot),
  plus Headunit Server / Self / Helper modes. Steering keys, GPS, mic, overlays.
- **Low-end stereo mode** (auto-on for ~2 GB RAM / slow CPU): music is never dropped to chase the
  picture, so Spotify stops stuttering. First run also picks **720p / 30 fps / AAC**, the stable
  profile for a 2.4 GHz screen — 60 fps is what makes a 2.4 GHz link drop into a connect/disconnect
  loop. Change these any time in Settings → Video.
- **AirPlay for iPhone** — the iPhone's music and mirrored screen over the stereo's Wi-Fi hotspot.
  No cable, no dongle, no Bluetooth data channel. Open it with the **CarHub iPhone** icon
  (manual by default).

## Why not wireless CarPlay?

CarPlay must start with a Bluetooth handshake to the iPhone, and many cheap stereos route Bluetooth
through the maker's own chip that apps cannot use — so wireless CarPlay cannot work there, by any
app. AirPlay runs over Wi-Fi instead and does the same job (music + screen).

## Build from source

JDK 21, Android SDK (platform 36, build-tools 36, NDK 29.0.14206865, CMake 3.22.1).

```bash
./gradlew assembleGithubRelease
```

`-PprebuiltNative=prebuilt-native` packages the bundled Open Headunit native libraries instead of
compiling them (same source, taken from the matching release). `-Pabis=armeabi-v7a` builds the
smaller 32-bit-only APK.

## License

AGPL-3.0 (the Open Headunit parts) and GPL-3.0 (the AirPlay parts). See `LICENSE`. If you share the
app, you must share the source too.
