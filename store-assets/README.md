# Store assets & submission checklist

Everything needed to publish SLOVO to the App Store and Google Play, minus the
steps that require your developer accounts.

```
store-assets/
├── icons/
│   ├── slovo-icon.svg            master vector
│   ├── play-icon-512.png         Play Console app icon (512×512)
│   └── appstore-icon-1024.png    App Store icon (1024×1024, no alpha)
├── feature-graphic/
│   ├── play-feature.svg
│   └── play-feature-1024x500.png Play Store feature graphic (required)
├── metadata/
│   ├── android/en-US/            title, short/full description, changelogs/ (supply layout)
│   └── ios/en-US/                name, subtitle, keywords, description, … (fastlane deliver layout)
├── screenshots/README.md         sizes + capture commands (must be shot on device)
└── privacy-policy.md             host this and use its URL in both stores
```

The launcher icons themselves are generated straight into the app by
`cd content-prep && npm run icons` — the master is the in-app Misha bear, so the
icon can never drift from the mascot. Re-run it if the bear ever changes.

## Done in-repo ✅
- App icons wired for Android (adaptive + legacy densities) and iOS (asset catalog).
- iOS display name = SLOVO, portrait-locked on iPhone.
- Release signing: `keystore.properties` (gitignored) + `slovo-upload.jks` generated;
  `assembleRelease` produces a signed APK. Template at `keystore.properties.example`.
- CC-BY audio attribution surfaced in-app (YOU → About → View Full Credits).
- Store listing copy (both platforms), privacy policy, feature graphic, store icons.

## Release process 📦

The iOS pipeline is automated — see [`../docs/release-ios.md`](../docs/release-ios.md)
for the full runbook (signing, `fastlane ios beta` / `release` / `submit`, screenshot
capture, and the submission gates). Android is
[`../docs/release-android.md`](../docs/release-android.md).

Still needing a human, once:

1. **Back up `slovo-upload.jks` + its passwords** somewhere safe (password manager).
   With Play App Signing this is the resettable *upload* key, but don't rely on that.
2. **Apple**: create the App Store Connect record for `cx.viz.slovo` and put its
   numeric App ID in `fastlane/.env` as `ASC_APP_ID`.
3. **Google**: create the Play Console app, enrol in Play App Signing, and upload the
   first AAB by hand. After that, create a service account JSON key and the
   `fastlane android play_*` lanes take over — see `../docs/release-android.md`.
4. **Web-only store forms**: Apple App Privacy ("No data collected" — answer *and*
   Publish) and Pricing → Free; Play **Data safety** ("no data collected").
5. **Enable GitHub Pages** (`main` → `/docs`) so the privacy and support URLs resolve.

## Build commands
```
# Android signed release APK / bundle
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
  ./gradlew :composeApp:assembleRelease        # or bundleRelease for the AAB Play wants

# iOS: open iosApp/iosApp.xcodeproj in Xcode, set your team, Product ▸ Archive
```
