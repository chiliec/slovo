# AGP 9 migration — DONE (2026-08-02)

Landed on `chore/dependency-refresh`. Gradle 8.11.1 → **9.6.1**, AGP 8.9.0 →
**9.3.1**, compileSdk 35 → **37**, lifecycle-viewmodel 2.10.0 → **2.11.0**.
`targetSdk` deliberately stayed at **35**.

Verified: 77 tests / 0 failures · `bundleRelease` AAB passes `jarsigner -verify`
· both iOS targets compile · `linkReleaseFrameworkIosArm64` links from clean.

> One caveat: the first `linkReleaseFrameworkIosArm64` run failed. Its log had
> been piped through a grep filter, so the root cause was not recoverable. Two
> subsequent runs — one incremental, one from `clean` — both succeeded. Treat it
> as unexplained rather than fixed; if it recurs, capture the full log and check
> linker memory against `org.gradle.jvmargs=-Xmx2048m`.

**The degraded-lint regression is fixed.** AGP 9's lint parses Kotlin 2.3/2.4
metadata, so `lintVitalAnalyzeRelease` no longer errors out on SQLDelight,
kotlinx-serialization and kotlin-stdlib.

## What actually broke, and how it was resolved

**1. AGP 9 refuses `com.android.application` + Kotlin Multiplatform in one module.**

```
The 'com.android.library' (or 'com.android.application') plugin is not compatible
with the 'org.jetbrains.kotlin.multiplatform' plugin since AGP 9.0.
```

AGP's recommended fix is `com.android.kotlin.multiplatform.library` — but that is
a *library* plugin, and `composeApp` is the application module that produces the
AAB while also producing the iOS framework. Taking the recommendation means
splitting into a KMP library module plus an Android application module, which
also drags in the Xcode project, the fastlane lanes, CI and the screenshot
tooling. Not worth doing while a release is in flight.

Used AGP's supported bypass in `gradle.properties` instead:

```properties
android.builtInKotlin=false
android.newDsl=false
```

**2. Manifest merge collision on `ComponentActivity`.**
`androidx.compose.ui:ui-test-manifest` 1.11.4 declares
`androidx.activity.ComponentActivity` with `android:exported="true"`, colliding
with `composeApp/src/debug/AndroidManifest.xml`, which registers the same
activity so Robolectric can resolve it.

Deleting the local declaration does **not** work — that AAR manifest is not
merged into the *unit-test* manifest, so all 21 Compose UI tests fail with
`Unable to resolve activity for Intent`. It *is* merged for the debug APK, which
is where the collision occurs. Resolved with `tools:replace="android:exported"`,
keeping `exported="false"` (correct for a test-only activity).

**3. `testOptions.unitTests.all { }`** went through AGP's obsolete variant API.
Moved to `tasks.withType<Test>().configureEach { }`.

## Remaining debt

- **The single-module layout stays. The split is DEFERRED — do not start it.**
  (Reassessed 2026-08-02.) The original reasoning was that `android.newDsl=false`
  re-enables the legacy variant API, which AGP says is **removed in AGP 10.0**.
  But see the next bullet: the calls into that API come from the **Kotlin Gradle
  Plugin**, not from this build script. AGP cannot remove it in 10.0 without
  breaking KGP, so either JetBrains ships a KGP targeting the new AGP DSL — which
  may make this single-module layout work again with no split at all — or the
  removal slips. Restructuring the Gradle build, the Xcode build phase, the
  fastlane lanes and CI against a deadline that may dissolve is not worth it.
  **Revisit when AGP 10 hits preview and KGP's status is visible.** The flags
  emit deprecation warnings on every build, so this will not be forgotten.

  Facts confirmed against the AGP 9.3.1 jar, for whenever it is picked up:
  - `com.android.kotlin.multiplatform.library` ships, but the only KMP DSL types
    are `KotlinMultiplatformAndroidLibraryExtension` / `...LibraryTarget`. There
    is **no** `...multiplatform.application`, so a KMP module structurally cannot
    produce an APK — a thin `com.android.application` module is mandatory, not a
    stylistic choice.
  - Source sets become `androidMain` / `androidHostTest` / `androidDeviceTest`.
  - `withHostTest { isIncludeAndroidResources = true }` exists, so Robolectric
    host tests can stay in the KMP module.
  - **Blocker 1:** the extension has no `buildConfig`, no `defaultConfig` and no
    build types, so `BuildInfo.android.kt`'s `actual val isDebugBuild =
    BuildConfig.DEBUG` will not compile. Cleanest fix: drop the `expect/actual`
    and make `isDebugBuild` an `AppModule` constructor property — `BuildConfig.DEBUG`
    from the app module's `MainActivity`, `Platform.isDebugBinary` from
    `MainViewController`. Only consumer is `YouScreen.kt:137`.
  - **Blocker 2:** no build types means no `debug` source set, so the
    `src/debug/AndroidManifest.xml` `ComponentActivity` declaration that the 21
    Compose UI tests need has no home. Either an `androidHostTest` manifest is
    honoured, or those tests move to the app module's `src/test/`.
  - Keeping the `composeApp/` directory as the KMP library avoids path churn for
    the 110 `.m4a` files, composeResources, `content-prep/*.mjs`, the docs and the
    Xcode phase `:composeApp:embedAndSignAppleFrameworkForXcode`; only the
    fastlane AAB path and the CI task would move. Renaming to `shared/` is
    cleaner but a much larger diff. Undecided.
- **The residual obsolete-API warnings are not ours.** `applicationVariants`,
  `testVariants` and `unitTestVariants` are called by the Kotlin Gradle Plugin's
  own Android integration (`KotlinAndroidPlugin` → `AndroidProjectHandler` →
  `forAllAndroidVariants`). Traced with `-Pandroid.debug.obsoleteApi=true`.
  Nothing to fix in this build script; it needs a KGP release that targets the
  new AGP DSL.
- **`android { }` accessor is deprecated** (`build.gradle.kts:101`), also a
  consequence of `newDsl=false`.
- **A full iOS archive/IPA still has not been rebuilt** — only the KMP framework
  link is verified. Run `fastlane ios beta` before shipping anything from this
  branch.
- **The CI SDK-install step is unverified.** `.github/workflows/ci.yml` now runs
  `sdkmanager "platforms;android-37.0"` because compileSdk 37 is newer than the
  ubuntu-latest runner image's preinstalled platform. This has not been exercised
  on a real runner — watch the first CI run on this branch.
- **`gradlew.bat` is not tracked** and the wrapper upgrade regenerates it. It was
  deleted again to keep the diff focused; re-delete after any future
  `./gradlew wrapper` run, or start tracking it deliberately.
