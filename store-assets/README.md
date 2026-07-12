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
│   ├── android/en-US/            title, short/full description (fastlane supply layout)
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

## You need to do (account-gated) 🔒
1. **Back up `slovo-upload.jks` + its passwords** somewhere safe (password manager).
   With Play App Signing this is the resettable *upload* key, but don't rely on that.
2. **Apple**: enroll in the Apple Developer Program, create the App Store Connect
   record for `cx.viz.slovo`, set `DEVELOPMENT_TEAM` in the Xcode project, archive
   and upload (Xcode or `fastlane deliver`).
3. **Google**: create the Play Console app, enroll in Play App Signing, build an
   AAB (`./gradlew :composeApp:bundleRelease`) and upload (or `fastlane supply`).
4. **Capture screenshots** on a device/simulator — see `screenshots/README.md`.
5. **Host `privacy-policy.md`** (e.g. GitHub Pages) and drop the URL + your support
   email into both listings and the policy's Contact section.
6. Fill Play **Data safety** ("no data collected") and Apple **Privacy Nutrition
   labels** ("Data Not Collected") — both true for this offline app.

## Build commands
```
# Android signed release APK / bundle
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
  ./gradlew :composeApp:assembleRelease        # or bundleRelease for the AAB Play wants

# iOS: open iosApp/iosApp.xcodeproj in Xcode, set your team, Product ▸ Archive
```
