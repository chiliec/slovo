# SLOVO — Android / Google Play Release Runbook

How to cut a Google Play release of SLOVO. This is the **Android** runbook; store
copy (shared with iOS) lives in [`../store-assets/`](../store-assets/) (see its
`README.md` and `metadata/`).

> Status: release **infra is wired and dry-run-validated** — signed APK + AAB build
> and verify locally (`CN=SLOVO`), store assets generated, and a `platform :android`
> fastlane pipeline (`play_stage` / `bundle` / `play_internal` / `play_listing` /
> `play_promote`) is in place. Not yet published: the remaining steps need a Google
> Play Console account, which does not exist yet.
>
> The two lanes that need no credentials — `bundle` and `play_stage` — were both run
> green on 2026-08-01. The upload lanes are **unexercised**: nothing can authenticate
> against Play until the account exists, so treat their first real run as untested.

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
  This is **not** auto-incremented — unlike the iOS build number, which the `beta`
  lane derives from TestFlight, nothing queries Play for the last code. Edit it by
  hand before each upload or Play will reject the AAB as a duplicate.
- `versionName` — human string shown to users. Currently `"1.0.0"`.

Keep `versionName` in sync with the iOS `MARKETING_VERSION` when releasing both;
both are `1.0.0` today.

Release notes live in `store-assets/metadata/android/en-US/changelogs/<versionCode>.txt`
(with a `default.txt` fallback). Add a file named for the new `versionCode` each
time you bump it, or supply will fall back to the generic default text.

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

These are **web-UI only** — there is no supply/API path for any of them, the same
trap the iOS side hit with App Privacy and Pricing.

- **Data safety:** No data collected, no data shared. (No network at runtime; progress
  and settings stay on-device in SQLDelight.) Concretely: answer *no* to "Does your app
  collect or share any of the required user data types?", and *no* to the encryption /
  deletion follow-ups, which disappear once nothing is collected.
- **App content / privacy policy:** `https://chiliec.github.io/slovo/privacy.html`
  (live via GitHub Pages, verified 200 on 2026-08-01) — same URL the App Store record uses.
- **Content rating (IARC questionnaire):** educational vocabulary app, no objectionable
  content → expect "Everyone / PEGI 3". Note the app bundles CC-BY third-party audio;
  IARC has no equivalent of Apple's content-rights declaration, so nothing to declare
  there, but the in-app credits screen (YOU → About → View Full Credits) is the
  attribution surface if a reviewer asks.
- **Target audience:** not directed at children (choose 13+ to avoid Families policy
  overhead), unless you intend otherwise.
- **Ads:** contains no ads.
- **Government / financial / health:** no to all.

---

## 6. First release — Internal testing track

The **first** upload has to be partly manual: the app record and Play App Signing
enrolment don't exist yet, and supply cannot create them.

1. Play Console → **Create app**: name `SLOVO`, English (US), App, Free. This is what
   reserves `cx.viz.slovo`.
2. Play Console → **Testing → Internal testing → Create new release**.
3. **Play App Signing:** accept "Use Google-generated key". Your `slovo-upload.jks`
   becomes the upload key. This is the default and recommended.
4. Upload `composeApp-release.aab` by hand this once.
5. Add testers (email list), save, review, **roll out to Internal testing**.

### Then switch to fastlane

Once the app record exists, create a service account so supply can authenticate:

1. Play Console → **Setup → API access → Create new service account**, which sends you
   to Google Cloud. Create the account, then create a **JSON key** for it.
2. Back in Play Console, grant that account **Release manager** (or at minimum: view
   app information, manage production/testing releases, manage store presence).
3. Save the JSON to the repo root as `play-service-account.json` — **gitignored**.
   For CI, base64 it into a `PLAY_JSON_KEY` secret instead.

Permission propagation is not instant; a fresh service account can 401 for a few
minutes before it starts working.

Then, from the repo root (fastlane needs rbenv Ruby 3.2.2+, not system Ruby):

```bash
export PATH="$HOME/.rbenv/shims:$PATH"
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools

bundle exec fastlane android play_stage      # no credentials needed — inspect the staged listing
bundle exec fastlane android play_internal   # build AAB + upload to internal, as a DRAFT
bundle exec fastlane android play_listing    # listing text/images only, no binary
bundle exec fastlane android play_promote    # internal -> production
```

Set `PLAY_VALIDATE_ONLY=1` to make any upload lane a dry run — Play validates the
payload and discards it. **Do this first**, before the first real upload.

`play_internal` uploads with `release_status: "draft"`, so nothing reaches testers
until someone clicks through in the console. `play_promote` takes `PLAY_ROLLOUT`
(a fraction, e.g. `0.1` for a 10% staged rollout; defaults to `1.0`).

Like the iOS `release` lane, the Android lanes keep `store-assets/` as the single
source of truth and stage a copy into supply's `<lang>/images/…` layout at run time
(`fastlane/play-metadata/`, gitignored, rebuilt each run). Never edit the staged tree.

---

## 7. Pre-upload checklist

- [ ] `versionCode` bumped (strictly greater than last uploaded) — **manual, not automated**
- [ ] `store-assets/metadata/android/en-US/changelogs/<versionCode>.txt` written
- [ ] Upload keystore in place, password set, **backed up**
- [ ] `bundle exec fastlane android bundle` succeeds
- [ ] `jarsigner -verify` reports "jar verified" with `CN=SLOVO`
- [ ] `bundle exec fastlane android play_stage` shows the expected listing tree
- [ ] `PLAY_VALIDATE_ONLY=1` dry run passes
- [ ] Privacy-policy URL live
- [ ] Data-safety / content-rating / target-audience forms answered **in the web UI**

---

## Known follow-ups (not blockers for internal testing)

- **`versionCode` is not auto-incremented.** The iOS `beta` lane derives its build
  number from TestFlight; there is no equivalent here, so a forgotten bump means a
  rejected upload. Worth wiring to `google_play_track_version_codes` once the account
  exists and the lane can actually be tested against Play.
- **R8/minify** — release ships un-minified (`isMinifyEnabled = false`). Enabling R8
  shrinks the APK but needs keep-rules verified against the KMP/SQLDelight/serialization
  stack; defer until there's a reason.
