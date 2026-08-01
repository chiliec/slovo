# SLOVO — iOS / App Store Release Runbook

How to cut a TestFlight or App Store release of SLOVO. The Android counterpart is
[`release-android.md`](release-android.md); store copy lives under
`../store-assets/metadata/`.

> **iOS has no free sideload.** Unlike Android, where a signed `.apk` installs
> directly, there is no way to hand an iPhone user a file. Reaching real users
> requires TestFlight or the App Store, both of which need the paid membership.

> **Status (2026-08-01):** `1.0.0` build 2 (App ID `6796900036`) is
> `WAITING_FOR_REVIEW` in App Store Connect — metadata, screenshots, age
> rating, content rights, pricing, and App Privacy are all set, and the
> version has been submitted. `automatic_release: false`, so **someone must
> click Release manually** in App Store Connect once Apple approves it.

## 0. Identifiers

| Thing | Value |
|---|---|
| Bundle ID | `cx.viz.slovo` |
| Apple Team ID | `7JF6XQC536` |
| ASC API Key ID | `948K3FKL2H` (account-wide, shared with the sibling app) |
| Marketing version | `1.0.0` (`MARKETING_VERSION`, both build configs) |
| Build number | auto-incremented from TestFlight; committed value is a floor |
| Apple App ID | set `ASC_APP_ID` in `fastlane/.env` after creating the record |

## 1. Machine setup

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
bundle install                       # gems land in vendor/bundle
cp fastlane/.env.example fastlane/.env   # then fill ASC_ISSUER_ID + ASC_APP_ID
cp ../indonesian-app/AuthKey_948K3FKL2H.p8 .   # gitignored
```

The Xcode build shells out to Gradle for `ComposeApp.framework`, so `JAVA_HOME`
must be exported before anything that archives. The iOS host uses the UIKit
lifecycle (`AppDelegate` + `SceneDelegate`) because Compose Multiplatform's
`PlistSanityCheck` requires it — do not convert it to SwiftUI `@main`.

## 2. Signing

One-time, local:

```bash
bundle exec fastlane ios signing_assets
```

Creates (or downloads) the Apple Distribution certificate and the App Store
provisioning profile. There is no gitignored secrets file on iOS the way Android
has `keystore.properties` — the private key lives in the login Keychain.

For CI, export the cert + key from Keychain Access as a `.p12` and
`base64 -i dist.p12 | pbcopy` into the `DIST_CERT_P12` secret.

## 3. Ship a build to TestFlight

Locally:

```bash
bundle exec fastlane ios beta
```

Or push a tag and let CI do it:

```bash
git tag v1.0.0 && git push origin v1.0.0
```

Either path fetches the profile, sets the build number to the highest already on
TestFlight plus one, archives with `gym`, and uploads with `pilot`. Bump
`MARKETING_VERSION` in Xcode by hand for a new user-facing version.

### GitHub secrets for CI

| Secret | Value |
|---|---|
| `ASC_KEY_ID` | `948K3FKL2H` |
| `ASC_ISSUER_ID` | Issuer ID (UUID) from ASC → Users and Access → Integrations |
| `ASC_KEY_P8` | `base64 -i AuthKey_948K3FKL2H.p8 \| pbcopy` |
| `DIST_CERT_P12` | `base64 -i dist.p12 \| pbcopy` |
| `DIST_CERT_PASSWORD` | the `.p12` export password (may be empty) |

`Gemfile.lock` is intentionally **not** committed: a lock resolved under local
Ruby 2.6 pins gems and a Bundler version that CI's Ruby 3.3 cannot install.

## 4. Screenshots

App Store Connect requires a **6.9-inch iPhone** set at **1320 × 2868**. The usual
smoke-test simulator (iPhone 16/17 Pro, 1206 × 2622) is *not* an accepted size —
use an **iPhone 16 Pro Max**.

Seed a realistic state first, otherwise the YOU and HOME screens are empty:

```bash
node scripts/gen-seed-sql.mjs > build/seed.sql
# launch once so the DB exists and migrates, then terminate so WAL is checkpointed
xcrun simctl launch booted cx.viz.slovo && sleep 3
xcrun simctl terminate booted cx.viz.slovo
DB=$(find "$(xcrun simctl get_app_container booted cx.viz.slovo data)" -name slovo.db)
sqlite3 "$DB" < build/seed.sql
xcrun simctl launch booted cx.viz.slovo
```

Six shots into `store-assets/screenshots/ios/`, driven with
`~/idb-venv/bin/idb ui tap <x> <y>` (device points = screenshot px ÷ 3):

1. `01-home.png` — HOME: Misha, streak ticker, REVIEW card, unit list
2. `02-study.png` — STUDY: Cyrillic + transliteration + PLAY
3. `03-quiz.png` — QUIZ: recognition question mid-answer
4. `04-you-overview.png` — YOU: OVERVIEW stats + MASTERY bars
5. `05-you-srs.png` — YOU: SPACED REPETITION charts
6. `06-credits.png` — CREDITS dialog

```bash
xcrun simctl io booted screenshot store-assets/screenshots/ios/01-home.png
```

Android phone shots must be **1200 × 2400**: raw device frames are 1080 × 2400
(2.22:1), which exceeds Play's 2:1 maximum and is rejected. Pad with the app's
Sand:

```bash
sips -p 2400 1200 --padColor F3EEE2 shot.png --out shot.png
```

## 5. App Store listing

```bash
bundle exec fastlane ios release   # text + screenshots, no binary, no submit
```

Sources: text from `store-assets/metadata/ios/`, screenshots from
`store-assets/screenshots/ios/` (staged into a gitignored
`fastlane/screenshots/en-US/` each run).

> **Review contact is required.** `deliver` must *create* the review-detail record
> on a first-ever version, and the API rejects that without
> `review_information/phone_number.txt` in `+<country> <number>` form. That file is
> gitignored — create it locally. The `Error fetching app store review detail - No
> data` line before it is benign.

## 6. Submission gates

| Gate | How |
|---|---|
| Age rating 4+ | `ruby scripts/asc_age_rating.rb` |
| Content rights | same script — `USES_THIRD_PARTY_CONTENT` (Tatoeba CC-BY audio) |
| Export compliance | `ITSAppUsesNonExemptEncryption=false` in `Info.plist` + the `submit` lane |
| Pricing → Free | web UI only (ASC → Pricing and Availability) |
| App Privacy → No data collected | web UI only, and **Publish** is a separate button from answering |

`ruby scripts/asc_state.rb` prints the current state read-only.

```bash
bundle exec fastlane ios submit     # attaches the latest build, submits for review
```

`automatic_release: false`, so **click Release manually** in App Store Connect
after approval.

## 7. Known fragilities

- The Xcode project has no shared `.xcscheme`; `xcodebuild` autocreates one from
  the single target, which is how the sibling's CI works. If `gym` ever reports
  "Couldn't find specified scheme", open the project in Xcode, tick **Shared** for
  the `iosApp` scheme, and commit
  `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`.
- The privacy-manifest required-reason list is a best guess until Apple's upload
  validator reports on it. Expect one adjust-and-re-upload cycle.
- Build 1 of `1.0.0`, uploaded 2026-08-01, passed the privacy-manifest validator
  clean on the first try — reached `VALID` with no compliance complaint.
- The first tag-triggered CI run failed at `get_provisioning_profile` with
  "Authentication credentials are missing or invalid," even though the same
  three ASC credentials worked in every local invocation. Root cause: the
  `ASC_ISSUER_ID`/`ASC_KEY_ID` secrets had been set via a shell pipe that left
  a trailing newline, corrupting the JWT's `iss`/`kid` claims. Fix: pipe
  `printf '%s'` (never `echo` or a bare `cut`/`grep` line) into
  `gh secret set` for every ASC secret. Build 2 uploaded clean afterward.
