# AGP 9 migration

Follow-up to the `chore/dependency-refresh` branch (2026-08-01). That branch took
every dependency it could to latest stable while staying on AGP 8.9.0. Four
things are left, and they are all coupled — none can land alone.

## Why it's blocked as one unit

| Want | Requires |
| --- | --- |
| `lifecycle-viewmodel-compose` 2.11.0 | compileSdk **37**, AGP **9.1+** |
| compileSdk 37 | AGP 9.x (AGP 8.9 caps recommended compileSdk at 35) |
| AGP 9.3.1 | Gradle **9.x** |
| Gradle 9.6.1 | — (also clears the KGP "Deprecated Gradle Version" warning) |

So the unit is: **Gradle 8.11.1 → 9.6.1, AGP 8.9.0 → 9.3.1, compileSdk 35 → 37,
lifecycle 2.10.0 → 2.11.0.**

## Second reason to do it: lint is currently degraded

`lintVitalAnalyzeRelease` now emits a wall of

```
Module was compiled with an incompatible version of Kotlin.
The binary version of its metadata is 2.4.0, expected version is 2.1.0.
```

AGP 8.9's bundled lint embeds a Kotlin 2.1 UAST parser, which cannot read the
Kotlin 2.3/2.4 metadata that SQLDelight 2.3.2, kotlinx-serialization 1.11.0 and
kotlin-stdlib 2.4.10 now ship. **The build still succeeds and the AAB is valid
and correctly signed** — but lint's analysis of those libraries is blind until
AGP ships a newer embedded compiler. AGP 9.3 does.

## Steps

1. `./gradlew wrapper --gradle-version 9.6.1` — commit the wrapper jar/properties.
2. Bump `agp = "9.3.1"` in `gradle/libs.versions.toml`.
3. Install the SDK platform: `sdkmanager "platforms;android-37"`. Only
   `android-35` and `android-36` are present on this machine.
4. Set `android-compileSdk = "37"`. **Leave `android-targetSdk = "35"`** — that
   is a separate, Play-policy-driven decision and changes runtime behaviour.
5. Bump `androidx-lifecycle-viewmodel = "2.11.0"` and drop the hold-back comment
   above it in the catalog.
6. Work through AGP 9 breaking changes in `composeApp/build.gradle.kts`. Known
   likely hits for this project:
   - AGP 9 has built-in Kotlin support that can conflict with the KMP plugin.
   - `buildFeatures { buildConfig = true }` — still required, verify it survives.
   - `namespace`, `signingConfigs`, and the `dependencies {}` block inside
     `android {}` may need reshaping.
   - The `testOptions.unitTests.all { systemProperty(...) }` hook (the
     Robolectric/SQLite JDBC driver workaround) uses the variant API — most
     likely single point of breakage.
7. Verify in this order, since each is a separate failure surface:
   - `./gradlew testDebugUnitTest` → expect **77 tests, 0 failures**
   - `./gradlew bundleRelease` → expect a signed AAB **and a clean lint run**
   - `./gradlew linkReleaseFrameworkIosArm64` (slow, ~13 min)
   - `./gradlew compileKotlinIosSimulatorArm64`
8. CI: `.github/workflows/ci.yml` is on JDK 21, which AGP 9 supports. No change
   expected, but confirm the run is green before merging.

## Do not forget

- A full iOS **archive/IPA** was never rebuilt on the dependency-refresh branch —
  only the KMP framework link was verified. Run `fastlane ios beta` (or a local
  `gym`) once before shipping any build produced after these bumps.
- `iosX64` was removed from the target list on the refresh branch. Nothing in the
  repo referenced it, but if a CI runner is ever moved to an Intel host it will
  need re-adding along with a Compose MP version that still publishes it (≤1.10).
