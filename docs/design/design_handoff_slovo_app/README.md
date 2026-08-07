# Handoff: SLOVO — Russian learning app (Kosmo direction)

## Overview
Complete product design for SLOVO, a gamified Russian-learning mobile app (iOS + Android). Covers onboarding with placement quiz, the core lesson loop, home/progress, monetization (SLOVO Plus), auth & recovery, social/invites, edge cases (offline, quit, streak loss), settings, icons/widgets, push copy, sound design, and store screenshots.

## About the Design Files
The `.dc.html` files in this bundle are **design references created in HTML** — interactive prototypes showing intended look and behavior, not production code to copy. The task is to **recreate these designs in the target codebase's environment** (SwiftUI/Kotlin, React Native, Flutter…) using its established patterns. If no codebase exists yet, pick the framework best suited to a cross-platform mobile app and implement there. Open each `.dc.html` directly in a browser; they are click-through interactive (`support.js`, `ios-frame.jsx`, `android-frame.jsx` are the prototype runtime/frames — ignore for implementation).

## Fidelity
**High-fidelity.** Colors, type, spacing, radii, shadows, copy, and interaction timings are final — recreate pixel-perfectly. Two exceptions (placeholder-level, listed under Assets): the Kosmo mascot SVGs and sound files.

## Screens / Views

### KosmoFlow.dc.html — core loop
- **Splash**: full-bleed Orbit Blue #4353FF, floating mascot + wordmark, twinkling diamond stars; slides up into Home.
- **Home**: cream #FFF6EA; top row = streak pill (comet icon + count) and hearts pill (red diamond + count), white pills, 2.5px ink border, hard shadow `0 3px 0 #2B2356`; unit banner (blue card, yellow eyebrow "UNIT 2", title 16/800); vertical lesson path — done nodes green 52px, current node yellow 74px "START" with 3px border + `0 5px 0` shadow, locked nodes white with 2.5px dashed rgba(43,35,86,.35); mascot floats beside path.
- **Lesson** (word bank → multiple choice → pair match → speaking): header = ✕ quit (opens confirm), progress bar (16px white pill, yellow fill, width transition .4s), hearts counter. Question card white 2.5px border radius 14 shadow `0 3px 0`. Word chips: white, radius 12, padding 8×14, 15/700; selected chips tint #E9EBFF border #4353FF. Answer feedback: correct = #DFF5E9 + border #3FBF7F, wrong = #FFE3DF + border #FF6B5E, others dim to 50% opacity; locks on tap, auto-advances after 900ms.
- **Mistake review**: same lesson chrome, re-drills failed words.
- **Celebration**: full-bleed blue, mascot, XP total, star twinkle, CONTINUE.

### KosmoOnboarding.dc.html
- **Welcome**: orbiting dashed ring (24s spin) around mascot, wordmark "SLOVO" 42/800 + yellow diamond, tagline, LET'S GO primary button, "I already have an account" link.
- **Goal / Level / Daily-orbit picks** ("MISSION BRIEFING · N OF 3" eyebrow): selectable cards — default white/ink, selected #E9EBFF/#4353FF; NEXT button disabled state = fill #F0E6D6, text rgba(43,35,86,.35), no shadow.
- **Placement quiz** (only if level ≥ "Some phrases"): 3 questions, same feedback tinting, 900ms advance; **Result**: yellow circle badge with recommended unit, lessons-skipped subtitle.
- **Streak commit**: day-chips row (day 1 filled comet, future dashed, day-7 badge), REMIND ME DAILY → native notification permission (iOS alert in SF Pro / Android M3 dialog in Roboto), MAYBE LATER.
- **Ready**: blue screen, «Поехали!» + transliteration, picked goal/level/minutes as chips, START LESSON 1.

### KosmoExtras.dc.html — monetization, auth, social, edge cases
- **Plus paywall**: full-bleed blue; ✕ close top-left; mascot + "SLOVO ◆ PLUS" wordmark; 4 benefit rows (yellow diamond bullets); 3 plan rows (1 MONTH $12.99/mo · 12 MONTHS $4.99/mo "SAVE 62%" badge, default-selected · FAMILY $8.33/mo), selected = cream fill + yellow 3px border + filled radio; CTA "START 7 DAYS FREE" (or "GET PLUS" without trial), price subline, Restore purchase / Terms links. **Success**: blue, floating PLUS diamond badge, "Welcome aboard, cosmonaut", CONTINUE.
- **Sign in / Sign up**: segmented pill tabs; inputs = white, 2.5px ink border, radius 14, shadow `0 3px 0`, 15/700; Forgot password? link (sign-in only); OR divider; Continue with Apple (black) + Google (white) — **Apple first on iOS, Google first on Android**; terms note on sign-up.
- **Password recovery**: email → "Check your inbox" state (floating envelope-in-yellow-circle, echoed email, 30-minute validity note, Resend link, BACK TO SIGN IN).
- **Friends**: weekly squad leaderboard rows (rank, initial avatar, name, XP; "You" row highlighted #E9EBFF/#4353FF); FOLLOW buttons (yellow → FOLLOWING green #DFF5E9/#3FBF7F); invite card (yellow) with referral link chip + COPY (→ "COPIED!" 1.5s) + INVITE FRIENDS → native share sheet (iOS 16px-radius sheet with link preview; Android 28px-radius "Share invite" sheet).
- **Offline**: mascot with grey antenna + ✕ badge, "Off the grid", saved-lessons card with green READY pills, sync note, RETRY CONNECTION (1.1s "SEARCHING…" then toast).
- **Quit confirm**: modal over dimmed lesson — "Wait, don't float away!", 60%-through warning, KEEP GOING (yellow) / QUIT LESSON (white, coral text).
- **Streak lost**: grey fizzled comet, "DAY 14 → DAY 0?", primary = USE STREAK FREEZE · N LEFT (if freezes > 0) or REPAIR WITH PLUS (links to paywall); secondary START OVER FROM ZERO; saved state = red/yellow comet "Streak saved!", reset state = blue comet "Fresh launch".

### SLOVO Visual Directions.dc.html — supporting specs
App icons (3 concepts, pick pending), home/lock widgets, push-notification copy ×5, sound-design cue table (section 8a), empty/error states, settings screen (Sounds/Haptics/etc. toggles), loading skeletons, splash→home transition spec.

### Kosmo Handoff Spec.dc.html
Rendered one-page spec: tokens, type specimens, live component samples, motion table, platform splits, screen map, asset checklist. Same values as this README.

### Kosmo Store Screenshots.dc.html
6 store panels at 6.7" ratio (430×932 logical; export @3× = 1290×2796; resize to 1080×2340 for Play).

## Interactions & Behavior
- **Press state (all buttons/tappable cards)**: instant translateY(2px) + shadow 4→2 (no transition down; springs back on release).
- **Answer feedback**: lock on first tap → tint/border swap → auto-advance 900ms.
- **Progress bars**: width transition .4s ease.
- **Toasts**: dark ink pill bottom-center, 1.7s auto-dismiss.
- **Ambient motion**: `kf-float` translateY 0→−7px 3–3.2s ∞ (mascot/hero marks); `kf-twinkle` opacity .2→1 + scale .8→1.15, 1.8s ∞ staggered .3–.9s (stars); `kf-spin` 24s linear (orbit ring).
- **Sound & haptics**: toy synth (sine + marimba), rewards rise in pitch, mistakes soften; lesson complete = 4-note liftoff arpeggio + Kosmo "boop" (1.2s); haptics mirror audio (light tap select / double-tap success / none on error); all duckable; toggles in Settings. Full cue table: Visual Directions §8a.
- **Onboarding routing**: level ≥ "Some phrases" → placement quiz, else straight to streak commit; quiz score → recommended start unit (0 correct→U1, 1–2→U2, 3→U3).

## State Management
- User profile: goal, level, daily-goal minutes (5/10/15/20 → 50–200 XP/day), start unit.
- Session: hearts (3), lesson progress %, mistake queue (re-drilled post-lesson), XP.
- Streak: day count, freeze inventory (repair = Plus perk, once/month), lost→saved/reset transitions.
- Subscription: free / trial / Plus (plan id); gates unlimited hearts, ads, offline downloads, streak repair.
- Social: friends + weekly XP, follows, referral link (`slovo.app/r/<CODE>`), invite reward (1 free Plus week both sides).
- Offline: downloaded lesson cache; leagues/friends sync deferred until online.

## Design Tokens
**Colors**: Cosmos Cream #FFF6EA (bg) · Ink #2B2356 (text/borders/shadows) · Star Yellow #FFC531 (primary/CTA/streak) · Orbit Blue #4353FF (links/eyebrows/moment screens) · Comet Coral #FF6B5E (hearts/errors) · Signal Green #3FBF7F (success) · tints: selected #E9EBFF, correct #DFF5E9, wrong #FFE3DF, disabled #F0E6D6 · mascot #F2A65A/#B96A34/#FFD9A8 · Night #101226 (widgets) · Deep Ink #1A1440 (shadow under dark buttons). Ink opacity steps: .55 body-sub, .45–.5 captions, .4 icons, .35 disabled, .15–.25 rules, .1 dividers. Max 2 screen backgrounds: cream for work, blue for moments.
**Structure**: border 2.5px solid Ink (2px chips, 3px hero); shadows hard, never blurred — `0 2px 0` chips, `0 3px 0` cards, `0 4px 0` buttons, `0 5–6px 0` hero; radius 10–12 chips, 14–16 cards, 15 buttons, 99 pills, 50% orbs; screen padding 18–22px sides, 24–26px bottom; stack gaps 10–14px; hit targets ≥44px.
**Type — Baloo 2 (500–800, latin + cyrillic subsets)**: wordmark 42/800 ls1 · display 37/800 · title 24/800 lh1.15 · card title 16/700 · button 15/800 CAPS ls.5 · body 13/600 ink-55% · eyebrow 12/800 CAPS ls2 blue · caption 11/600 ink-45%. Nothing below 10px. System dialogs use native SF Pro / Roboto, never Baloo.
**Platform splits**: top safe pad 58px iOS / 14–16px Android; back glyph ‹ / ←; sheet radius 16px / 28px; auth button order Apple-first / Google-first. Everything else shared.

## Copy Voice
Buttons & eyebrows ALL CAPS verb-first; body sentence case, warm, ≤2 lines; one space metaphor per screen; Russian in «guillemets», blue screens add transliteration + gloss; mistakes get soft copy, never shame; no emoji — the rotated-square diamond is the only ornament.

## Assets (open items — placeholders in the prototypes)
1. Kosmo mascot final art, 5 poses: idle float, celebrating, sad, offline, gold-visor Plus (current inline SVGs are placeholders).
2. App icon: pick 1 of 3 concepts (Visual Directions §5a), export iOS set + Android adaptive layers.
3. Widget assets (§5b, §6b).
4. Sound pack: 6 cues per §8a table.
5. Native RU voiceover + TTS fallback for listening/speaking exercises.

## Files
- `KosmoFlow.dc.html` — splash, home, lesson loop, mistake review, celebration (interactive)
- `KosmoOnboarding.dc.html` — onboarding + placement + permission (interactive)
- `KosmoExtras.dc.html` — paywall, auth, recovery, friends/invites, offline/quit/streak (interactive)
- `SLOVO Visual Directions.dc.html` — icons, widgets, push copy, sound table, empty/error, settings, skeletons
- `Kosmo Handoff Spec.dc.html` — rendered token/component spec
- `Kosmo Store Screenshots.dc.html` — 6-panel store set
- `support.js`, `ios-frame.jsx`, `android-frame.jsx` — prototype runtime/frames only (not for implementation)
