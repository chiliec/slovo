# SLOVO

Offline Russian words-&-phrases trainer. Kotlin Multiplatform + Compose Multiplatform (Android + iOS).

## Install (Android)

Scan the QR code or open the [latest release](https://github.com/chiliec/slovo/releases/latest) on your phone, download the APK, and tap to install. Requires **Android 7.0+**. Fully offline — no account, no network, no data collected.

<img src="docs/install-qr.png" width="200" alt="QR code to the latest SLOVO Android release" />

> On first install, Android asks you to allow "install unknown apps" for your browser — that's expected for apps distributed outside the Play Store.

> iOS is not yet distributed (requires an Apple Developer account).

To cut a release, see [`docs/release-android.md`](docs/release-android.md).

## Run — Android
1. Install the Android SDK; set `sdk.dir` in `local.properties`.
2. `./gradlew :composeApp:installDebug` (device/emulator running), or open in Android Studio and Run.

## Run — iOS
Open `iosApp/iosApp.xcodeproj` in Xcode and run (the Compose framework builds via Gradle).

## Content
Content is bundled from `content-prep/seed.yaml`. To regenerate:
`cd content-prep && npm install && npm run prep` (needs `ffmpeg` and `curl`).
All audio is CC-BY from Tatoeba — see `composeApp/src/commonMain/composeResources/files/ATTRIBUTION.md`.
