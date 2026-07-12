# Screenshots — capture plan

Screenshots must be captured on a device/simulator (they can't be generated
here). Aim for 5–6 that tell the story: Home → Lesson study → Quiz → Progress →
Credits.

## Required sizes

### App Store (per Apple, at least one set is required)
- **6.9" iPhone** (e.g. iPhone 16 Pro Max) — 1320 × 2868 px  ← primary, required
- **6.5" iPhone** (e.g. iPhone 11 Pro Max) — 1242 × 2688 px  ← still requested by some flows
- iPad 12.9" — 2048 × 2732 px (only if you ship iPad; `TARGETED_DEVICE_FAMILY = 1,2` currently includes iPad)

### Google Play
- **Phone**: min 2, 1080 × 1920 px (or device-native), PNG/JPEG, 16:9 or 9:16
- **Feature graphic** (required): 1024 × 500 px — see `feature-graphic/`
- Icon (512 × 512) is uploaded separately — already generated at
  `store-assets/icons/play-icon-512.png`

## Suggested shots
1. HOME — units grid with Misha + streak ticker
2. LESSON — a phrase card with Cyrillic + transliteration + play button
3. QUIZ — a recognition/recall question mid-answer
4. YOU — OVERVIEW stats + MASTERY bars
5. YOU — SPACED REPETITION charts
6. (optional) CREDITS dialog, to highlight real human audio

## How to capture

### Android (emulator `slovo_test` already exists)
```
adb shell am start -n cx.viz.slovo/.MainActivity
adb exec-out screencap -p > store-assets/screenshots/android/01-home.png
```

### iOS (simulator)
```
xcrun simctl io booted screenshot store-assets/screenshots/ios/01-home.png
```

Consider adding a colored caption band above each frame (many stores allow
framed marketing screenshots) using the sand/yellow palette.
