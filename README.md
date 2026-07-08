# SLOVO

Offline Russian words-&-phrases trainer. Kotlin Multiplatform + Compose Multiplatform (Android + iOS).

## Run — Android
1. Install the Android SDK; set `sdk.dir` in `local.properties`.
2. `./gradlew :composeApp:installDebug` (device/emulator running), or open in Android Studio and Run.

## Run — iOS
Open `iosApp/iosApp.xcodeproj` in Xcode and run (the Compose framework builds via Gradle).

## Content
Content is bundled from `content-prep/seed.yaml`. To regenerate:
`cd content-prep && npm install && npm run prep` (needs `ffmpeg` and `curl`).
All audio is CC-BY from Tatoeba — see `composeApp/src/commonMain/composeResources/files/ATTRIBUTION.md`.
