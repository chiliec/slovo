# AGENTS

## Map
- `composeApp/src/commonMain/kotlin/com/axveer/slovo/domain` — pure logic (models, QuestionFactory, calculators). No Compose/SQL/network. Fully unit-tested.
- `.../data` — repositories over SQLDelight + bundled JSON. `expect` DriverFactory.
- `.../platform` — expect/actual: AudioPlayer, Clock (todayEpochDay).
- `.../ui` — Compose: MISHA theme + components + screens + navigation. Manual DI via `AppModule`.
- `composeApp/src/commonMain/sqldelight/...` — DB schema.
- `content-prep/` — Node/TS build-time pipeline (seed.yaml → JSON + audio).

## Conventions
- Never name a type `Unit` (Kotlin builtin) — use `LearnUnit`.
- Keep `domain/` pure and TDD'd; put time/IO behind `platform/` seams.
- No runtime network. All content is bundled and offline.
- Every audio clip is CC-BY from Tatoeba and MUST be listed in ATTRIBUTION.md.
- Commit messages: conventional style, no Co-Authored-By trailer.

## Add phrases
1. Edit `content-prep/seed.yaml` (verify each `tatoebaAudioId` is CC-BY Russian — see content-prep/README).
2. `cd content-prep && npm run prep`.
3. Rebuild the app; commit the regenerated `composeResources/files/*` and updated ATTRIBUTION.md.
