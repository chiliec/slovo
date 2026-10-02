# AGENTS

## Map
- `composeApp/src/commonMain/kotlin/cx/viz/slovo/domain` — pure logic (models, QuestionFactory, calculators). No Compose/SQL/network. Fully unit-tested.
- `.../data` — repositories over SQLDelight + bundled JSON. `expect` DriverFactory.
- `.../platform` — expect/actual: AudioPlayer, Clock (todayEpochDay).
- `.../ui` — Compose: MISHA theme + components + screens + navigation. Manual DI via `AppModule`.
- `composeApp/src/commonMain/sqldelight/...` — DB schema. `databases/*.db` are generated schema snapshots; regenerate with `generateCommonMainSlovoDatabaseSchema`, never hand-edit.
- `content-prep/` — Node/TS build-time pipeline (seed.yaml → JSON + audio).

## Conventions
- Never name a type `Unit` (Kotlin builtin) — use `LearnUnit`.
- Keep `domain/` pure and TDD'd; put time/IO behind `platform/` seams.
- All content is bundled and offline. The only runtime network call is `platform/Analytics.kt` — a fire-and-forget POST of the current route to self-hosted Umami, off by default and switched on only in `MainActivity`/`MainViewController` (never in tests). Keep it that way; don't add other network code without updating `docs/privacy.html`, the store listings and the Data safety / App Privacy answers.
- Changing a `.sq` schema ALWAYS needs a matching `.sqm` migration — existing installs are never re-created, only migrated. `./gradlew :composeApp:verifySqlDelightMigration` enforces this and runs in CI; after a legitimate schema change, add the `.sqm` and commit the new `databases/<version>.db`.
- Every audio clip is CC-BY from Tatoeba and MUST be listed in ATTRIBUTION.md.
- Commit messages: conventional style, no Co-Authored-By trailer.

## Add phrases
1. Edit `content-prep/seed.yaml` (verify each `tatoebaAudioId` is CC-BY Russian — see content-prep/README).
2. `cd content-prep && npm run prep`.
3. Rebuild the app; commit the regenerated `composeResources/files/*` and updated ATTRIBUTION.md.
