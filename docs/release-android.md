# SLOVO — Android / Google Play Release Runbook

How to cut a Google Play release of SLOVO. This is the **Android** runbook; store
copy (shared with iOS) lives in [`../store-assets/`](../store-assets/) (see its
`README.md` and `metadata/`).

> Status: release **infra is wired and dry-run-validated** — signed APK + AAB build
> and verify locally (`CN=SLOVO`); store assets generated. Not yet published: the
> remaining steps need a Google Play Console account, which does not exist yet.

---

## 0. One-time prerequisites (need a human + money)

1. **Google Play Console account** — one-time US$25. https://play.google.com/console
   Sign up, accept the Developer Distribution Agreement, complete identity
   verification (can take a couple of days).
2. **Create the app** in the console: name `SLOVO`, default language English (US),
   type App, Free. This reserves the package name **`cx.viz.slovo`** on first upload.

Nothing below can reach the store until step 0 is done. Everything else is already
prepared in-repo.

---

## 1. Signing model (already wired)

We use **Play App Signing**: Google holds the *app signing key*; we sign uploads
with an *upload key*. The build reads the upload key from a **gitignored**
`keystore.properties` at the repo root (see `composeApp/build.gradle.kts`). When
that file is absent, release builds are produced **unsigned** — so CI and clean
checkouts still assemble without the secret.

`keystore.properties` format (template committed as `keystore.properties.example`):

```properties
storeFile=slovo-upload.jks
storePassword=<store password>
keyAlias=slovo
keyPassword=<key password>
```

Both `keystore.properties` and `*.jks` / `*.keystore` are in `.gitignore` — **never
commit them**. The current `slovo-upload.jks` (cert `CN=SLOVO, O=viz.cx, C=US`) was
generated for release; regenerate before first upload only if you want a different
key:

```bash
keytool -genkeypair -v -keystore slovo-upload.jks -alias slovo \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=SLOVO, O=viz.cx, C=US"
# enter a strong password when prompted; put it in keystore.properties
```

**Back up the `.jks` file and its passwords** somewhere durable (password manager +
offsite). With Play App Signing this is the resettable *upload* key, but don't rely
on the reset flow — losing it before enrolling would mean you can never update the app.

---

## 2. Versioning

Set in `composeApp/build.gradle.kts` `defaultConfig`:

- `versionCode` — integer, **must strictly increase** with every uploaded build.
  Currently `1`. Bump by 1 each upload (even for re-uploads to the same track).
- `versionName` — human string shown to users. Currently `"1.0"`.

Keep `versionName` in sync with the iOS `MARKETING_VERSION` when releasing both.

---

## 3. Build the release artifact

Java 21 is required; export `JAVA_HOME` (and `ANDROID_HOME`) first.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew :composeApp:bundleRelease      # AAB for Play
./gradlew :composeApp:assembleRelease    # universal APK for side-load / dist/
```

Outputs:

```
composeApp/build/outputs/bundle/release/composeApp-release.aab   # upload to Play
composeApp/build/outputs/apk/release/composeApp-release.apk      # universal, side-loadable
```

### Verify it is signed with the upload key

```bash
jarsigner -verify -certs \
  composeApp/build/outputs/bundle/release/composeApp-release.aab
# expect: "jar verified." and cert CN=SLOVO
```

(Dry run on 2026-07-13 produced a ~9 MB AAB and ~9.5 MB APK, `jar verified`,
cert `CN=SLOVO`.)

### Distributable APK

For direct install (before Play, or for testers), copy the signed universal APK to
the gitignored `dist/` folder:

```bash
cp composeApp/build/outputs/apk/release/composeApp-release.apk dist/slovo-1.0.apk
```

> Play delivers per-device APKs from the AAB, so an `.aab` can't be `adb install`ed
> directly. The `assembleRelease` APK is universal and installs on any device
> (`adb install dist/slovo-1.0.apk`). Release has `isMinifyEnabled = false`, so there
> is no R8 divergence from debug.

---

## 4. Store listing assets (already generated)

Live under [`../store-assets/`](../store-assets/):

| Asset | Requirement | File |
|---|---|---|
| App icon | 512×512, 32-bit PNG | `store-assets/icons/play-icon-512.png` |
| Feature graphic | 1024×500, PNG/JPG | `store-assets/feature-graphic/play-feature-1024x500.png` |
| Phone screenshots | 2–8, must be shot on device | `store-assets/screenshots/` (see its README) |

Text fields (title, short/full description) — copy verbatim from
`store-assets/metadata/android/en-US/`.

---

## 5. Play Console content forms

SLOVO is fully offline and collects nothing — these forms are quick:

- **Data safety:** No data collected, no data shared. (No network at runtime; progress
  and settings stay on-device in SQLDelight.)
- **App content / privacy policy:** Play requires a privacy-policy URL. Host
  `store-assets/privacy-policy.md` (e.g. GitHub Pages) and use its URL. **← still to do.**
- **Content rating (IARC questionnaire):** educational vocabulary app, no objectionable
  content → expect "Everyone / PEGI 3".
- **Target audience:** not directed at children (choose 13+ to avoid Families policy
  overhead), unless you intend otherwise.
- **Ads:** contains no ads.
- **Government / financial / health:** no to all.

---

## 6. First release — Internal testing track

Recommended path for the first upload (fastest review, up to 100 testers):

1. Play Console → **Testing → Internal testing → Create new release**.
2. **Play App Signing:** accept "Use Google-generated key". Your `slovo-upload.jks`
   becomes the upload key. This is the default and recommended.
3. Upload `composeApp-release.aab`.
4. Fill release name (e.g. `1.0 (1)`) and release notes.
5. Add testers (email list), save, review, **roll out to Internal testing**.
6. Share the opt-in URL with testers; they install via Play.

Promote Internal → Closed → Open/Production from the console when ready. Production
requires the full store listing + all content forms complete.

---

## 7. Pre-upload checklist

- [ ] `versionCode` bumped (strictly greater than last uploaded)
- [ ] Upload keystore in place, password set, **backed up**
- [ ] `./gradlew :composeApp:bundleRelease` succeeds
- [ ] `jarsigner -verify` reports "jar verified" with `CN=SLOVO`
- [ ] Screenshots + feature graphic + 512 icon on hand
- [ ] Store text copied from `store-assets/metadata/android/en-US/`
- [ ] Privacy-policy URL live
- [ ] Data-safety / content-rating / target-audience forms answered

---

## Known follow-ups (not blockers for internal testing)

- **Privacy-policy URL** — author + host before Production (and for the App content form).
- **R8/minify** — release ships un-minified (`isMinifyEnabled = false`). Enabling R8
  shrinks the APK but needs keep-rules verified against the KMP/SQLDelight/serialization
  stack; defer until there's a reason.
- **Contact email** — drop a support address into the privacy policy's Contact section
  and both store listings.
