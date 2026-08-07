# Kosmo redesign — implementation plan (offline slice)

Redesign of SLOVO to the "Kosmo" cosmonaut visual direction + the offline-compatible
feature set. Backend-dependent features (auth, social/leaderboard, paywall,
offline-sync) are **explicitly out of scope** — the app stays 100% local/offline.

Design reference: extracted to **`docs/design/design_handoff_slovo_app/`** (also the raw
`design.zip` at repo root). Tokens/spec are in its `README.md` and
`Kosmo Handoff Spec.dc.html`. `.dc.html` = HTML prototypes, not code to copy — recreate
in Compose using existing patterns.

## Prep status (done 2026-08-07, before implementation)
- Design reference extracted to `docs/design/`.
- **Font decision made — Nunito, not Baloo 2.** The spec claims "Baloo 2 (latin +
  cyrillic)" but **Baloo 2 has zero Cyrillic glyphs** (verified: block 0x400–0x4FF
  absent; so do Fredoka). The app's content is Russian, and Russian interleaves with
  English at the chip level (word-bank chips, MC options, mascot bubbles), so a per-word
  font split is impractical → **one Cyrillic-capable rounded font everywhere: Nunito**
  (220 Cyrillic glyphs, weights 200–1000 so the full 500–800 scale works). User-approved.
- **Nunito static weights already staged** in `composeApp/src/commonMain/composeResources/
  font/`: `Nunito-Medium.ttf` (500), `Nunito-SemiBold.ttf` (600), `Nunito-Bold.ttf` (700),
  `Nunito-ExtraBold.ttf` (800) — instanced from the variable font, Cyrillic intact,
  correct `usWeightClass`. OFL license at `composeResources/files/Nunito-OFL.txt`.
  → Phase 0 is unblocked. Old fonts (`ArchivoBlack-Regular.ttf`, `SpaceGrotesk-*.ttf`)
  stay until Type.kt is rewritten, then delete them.

## Key insight
Kosmo is the **same neo-brutalist idiom the app already uses** (thick ink borders, hard
offset shadows `0 3px 0`, bordered pill chips, drawn mascot). The reskin is mostly
re-tokening, not rebuilding. Keep the `Misha*` component/type names — renaming to
`Kosmo*` touches every call site for zero behavior change (skip it).

## Design tokens (from design README)
- **Colors**: Cream `#FFF6EA` (bg) · Ink `#2B2356` (text/border/shadow) · Star Yellow
  `#FFC531` (primary/CTA/streak) · Orbit Blue `#4353FF` (links/eyebrows/moment screens) ·
  Comet Coral `#FF6B5E` (hearts/errors) · Signal Green `#3FBF7F` (success). Tints:
  selected `#E9EBFF`, correct `#DFF5E9`, wrong `#FFE3DF`, disabled `#F0E6D6`. Mascot
  `#F2A65A`/`#B96A34`/`#FFD9A8`.
- **Structure**: border 2.5px ink (2px chips, 3px hero); hard shadows never blurred —
  `0 2px 0` chips, `0 3px 0` cards, `0 4px 0` buttons, `0 5–6px 0` hero; radius 10–12
  chips, 14–16 cards, 15 buttons, 99 pills; screen pad 18–22px sides.
- **Type — Nunito** (see Prep status; spec said Baloo 2 but it has no Cyrillic). Scale:
  wordmark 42/800 · display 37/800 · title 24/800 · card title 16/700 · button 15/800
  CAPS ls.5 · body 13/600 ink-55% · eyebrow 12/800 CAPS ls2 blue · caption 11/600
  ink-45%. Nothing below 10px.
- **Platform splits**: top safe pad 58px iOS / 14–16px Android; back glyph ‹ / ←;
  auth order N/A (deferred). App already handles the iOS/Android pad split.

## Phases (ordered by dependency; each is independently reviewable)

### Phase 0 — Reskin foundation (do first, ships on its own) — DONE 2026-08-07
Reskins every existing screen for free. Verified: `compileDebugKotlinAndroid` clean.
No emulator/simulator on this machine to eyeball it (AVD + iOS runtime both cleared
since last session) — visual QA is still open, do it next time a device is available.
- `ui/theme/Color.kt` — swap `object Slovo` palette to Kosmo values above (keep symbol
  names or add Kosmo names + alias; whichever is the smaller diff at the call sites).
- `ui/theme/Type.kt` — wire the already-staged **Nunito** weights (Medium/SemiBold/Bold/
  ExtraBold) via `Font(Res.font.Nunito_*)`, replace Archivo Black + Space Grotesk, match
  the type scale, then delete the old TTFs. (Fonts are staged — see Prep status.)
- `ui/theme/Theme.kt` — update `lightColorScheme` mapping (primary=Yellow, bg=Cream…).
- `ui/components/Misha.kt` — border 3dp→2.5dp, verify shadow offsets match token table;
  replace `MishaMascot` bear with the **cosmonaut** (redraw the design's inline SVG as a
  Compose `Path`/`Canvas` — helmet ring, visor, antenna with coral tip).
- Font asset already handled (Nunito staged). Verify glyphs render in the emulator early.

### Phase 1 — Splash + Celebration
- **Splash**: full-bleed Orbit Blue, floating mascot + "SLOVO ◆" wordmark, twinkling
  diamond stars; slides up (`translateY(-103%)`, .45s) into Home. New composable, shown
  on cold start before the NavHost content.
- **Celebration**: full-bleed blue moment screen — "LIFTOFF!", XP earned + accuracy
  stat cards, "DAY N — STREAK SAFE" pill, CONTINUE. Replaces/augments the current Lesson
  `RESULT` phase in `LessonScreen.kt`.
- Ambient motion: `kf-float` (translateY 0→−7px, 3.2s ∞), `kf-twinkle` (opacity+scale,
  1.8s ∞ staggered). Implement as `rememberInfiniteTransition`.

### Phase 2 — Hearts
- New pure domain object `domain/Hearts.kt` (sibling to `StreakCalculator`): 3 hearts,
  −1 per wrong answer / pair mismatch, lesson fails at 0. **Session-only state** in
  `LessonViewModel` — no persistence needed for the offline slice (no regen/Plus).
  `ponytail:` per-lesson reset; add cross-session regen only if product wants it.
- Wire hearts counter into the lesson header (coral heart SVGs, dim at 18% when spent).
- Add a "out of hearts" path → exit lesson (no paywall; just back to Home).

### Phase 3 — Lesson loop redesign (word-bank → MC → pair-match → speaking)
- Extend `QuestionMode` in `domain/Models.kt`: add `WORD_BANK`, `PAIR_MATCH`, `SPEAK`.
- `domain/QuestionFactory.kt` — build the new question types (word-bank: shuffled chip
  set incl. distractors, target = correct token order; pair-match: 3 RU/EN pairs).
  Keep the existing mastery-gating logic.
- `ui/screens/LessonScreen.kt` — the four step UIs (see `KosmoFlow.dc.html`):
  - word-bank: answer tray (dashed) + chip bank, tap to add/remove, CHECK button.
  - multiple-choice: 3 option rows, selected tint `#E9EBFF`.
  - pair-match: 2-col grid, tap RU then EN; match→green, mismatch→coral shake + −1 heart.
  - speaking: mic button + "SAY IT OUT LOUD". **`ponytail:` no real ASR** — mirror the
    design prototype (record → auto-accept after ~1.6s) + "CAN'T SPEAK RIGHT NOW" skip.
    Real speech recognition is a later `expect/actual`; note the ceiling in code.
- Feedback bottom-sheet: correct `#DFF5E9`/green, wrong `#FFE3DF`/coral, lock on first
  tap, auto-advance 900ms. Progress bar width transition .4s.
- **Mistake review** screen: re-drills failed items before Celebration ("N to patch up",
  DRILL MISTAKES +10 XP, or skip). Reuses the lesson chrome.

### Phase 4 — Onboarding + placement quiz
- New flow `ui/screens/onboarding/` (see `KosmoOnboarding.dc.html`): Welcome → Goal →
  Level → Daily-orbit (5/10/15/20 min → 50–200 XP/day) → **placement quiz only if level ≥
  "some phrases"** → streak commit → Ready («Поехали!»).
- Placement: 3 questions via existing `QuestionFactory`; score → start unit (0→U1,
  1–2→U2, 3→U3).
- Persist profile: new `sqldelight/.../UserProfile.sq` (goal, level, dailyGoalMinutes,
  startUnit, onboarded flag) + `ProgressRepository` methods. Gate app entry: unonboarded
  → onboarding; onboarded → Home.
- Notification-permission ask = platform `expect/actual`, or defer to a Settings toggle.

### Phase 5 — Streak-freeze / streak-lost / quit-confirm
- Streak-freeze inventory in `user_stats` (`Stats.sq` + `StreakCalculator`): local, no
  Plus. On a broken streak → **streak-lost** screen: USE STREAK FREEZE · N LEFT (if >0)
  or START OVER (drop "repair with Plus" — no paywall in this slice).
- **Quit-confirm** modal over dimmed lesson: "don't float away", KEEP GOING / QUIT LESSON.

### Phase 6 — Settings + sound/haptics
- `ui/screens/SettingsScreen.kt` — Sounds / Haptics / Notifications toggles → local prefs
  (small `Settings.sq` row or multiplatform-settings; a `.sq` row matches current stack).
- `expect SoundPlayer` + `expect Haptics` (androidMain/iosMain actuals). Toy-synth cue
  table in Visual Directions §8a. **Assets (6 cues) are placeholder open-items** — wire
  the plumbing, drop sounds in later. Haptics mirror audio (light select / double success
  / none on error). All duckable, toggle-gated.

## Deferred (not this slice)
Auth + password recovery · Friends/real League/leaderboard (League tab stays a stub) ·
SLOVO Plus paywall/subscriptions · offline-sync "off the grid" states · widgets · app
icons · store screenshots. All require a backend or are separate release-asset work.

## Assets still needed (design open items)
1. ~~Font~~ — **DONE**, Nunito staged (see Prep status).
2. Cosmonaut mascot — redraw in Compose from the design SVG (5 poses: idle/celebrate/
   sad/offline/plus; offline+plus poses unused in this slice). The design's inline SVG
   (in `docs/design/.../KosmoFlow.dc.html`) is the source geometry.
3. Sound pack (6 cues) — Phase 6, placeholder-level.

## Notes for the implementer
- No androidx ViewModel in use — each screen owns a plain class with `mutableStateOf` +
  its own `CoroutineScope` (`remember`/`DisposableEffect`). New screens follow suit.
- DI is hand-rolled in `ui/AppModule.kt` (constructed in `MainActivity`/`MainViewController`).
- Nav is JetBrains `navigation-compose`, hand-built bottom bar in `ui/App.kt` — add a
  `Dest` + `composable{}` per new screen.
- Every new `domain/` object gets a `commonTest` (matches existing convention: 84 tests).
- Content is data-driven JSON (110 phrases / 8 units / 24 lessons) — no content changes
  needed; new question modes read the same cards.
