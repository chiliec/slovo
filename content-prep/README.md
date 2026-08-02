# Content Prep Pipeline

The `content-prep/` pipeline is a Node.js/TypeScript build-time tool that transforms `seed.yaml` into bundled JSON content and audio assets for the app.

## Pipeline Flow

1. **Input**: `seed.yaml` — human-editable YAML with units, lessons, and phrases
2. **Processing**:
   - Cyrillic → Latin transliteration (`translit.ts`)
   - Download audio from Tatoeba via ID
   - Transcode to AAC/m4a (64 kbps)
   - Generate JSON manifests matching Task 10's serializers
   - Build attribution list (CC-BY credits)
3. **Output**:
   - `composeApp/src/commonMain/composeResources/files/content/manifest.json` — unit index
   - `composeApp/src/commonMain/composeResources/files/content/<unitId>.json` — lessons & cards
   - `composeApp/src/commonMain/composeResources/files/audio/<cardId>.m4a` — audio clips
   - `composeApp/src/commonMain/composeResources/files/ATTRIBUTION.md` — CC-BY credits

## Seed Schema

`seed.yaml` structure:

```yaml
units:
  - id: string                 # e.g. "unit-greetings"
    title: string              # e.g. "Greetings"
    lessons:
      - id: string             # e.g. "greet-hello"
        title: string          # e.g. "Hello & goodbye"
        kind: VOCAB | LISTENING | RECALL
        phrases:
          - russian: string    # e.g. "Привет!"
            english: string    # e.g. "Hi!"
            tatoebaAudioId: number  # verified CC-BY Tatoeba sentence ID
            speaker: string    # (optional) Tatoeba username
            translit: string   # (optional) explicit transliteration; auto-generated if omitted
            note: string       # (optional) extra context for learner
```

### JSON Output Schema

**manifest.json**:
```json
{
  "units": [
    { "id": "unit-greetings", "title": "Greetings", "lessonCount": 2 }
  ]
}
```

**<unitId>.json**:
```json
{
  "lessons": [
    { "id": "greet-hello", "title": "Hello & goodbye", "kind": "VOCAB", "cardIds": ["greet-hello-0", "greet-hello-1"] }
  ],
  "cards": [
    {
      "id": "greet-hello-0",
      "russian": "Привет!",
      "transliteration": "Privet!",
      "english": "Hi!",
      "audio": "greet-hello-0.m4a",
      "note": "informal"
    }
  ]
}
```

## Finding Valid Tatoeba Audio IDs

All audio **must** be CC-BY from Tatoeba. To curate phrases:

```bash
cd /tmp
curl -L -o rus_audio.tar.bz2 https://downloads.tatoeba.org/exports/per_language/rus/rus_sentences_with_audio.tsv.bz2
bunzip2 -f rus_audio.tar.bz2
# columns: sentenceId <tab> username <tab> license <tab> attributionUrl
grep -iP '\tCC BY' rus_sentences_with_audio.tsv | head -40
```

Example output (keep the numeric IDs):
```
111111	someuser	CC BY	https://tatoeba.org/en/sentences/show/111111
222222	otheruser	CC BY	https://tatoeba.org/en/sentences/show/222222
```

Extract the sentence ID and lookup the Russian text:
```bash
# Using the ID (e.g., 111111), fetch the sentence details
curl -L https://tatoeba.org/en/sentences/show/111111 | grep -oP '"text":"[^"]*"' | head -1
```

Pick beginner-friendly, short sentences. Update `seed.yaml` with each ID as `tatoebaAudioId`, the Russian as `russian`, English gloss as `english`, and the username as `speaker`.

## Run the Pipeline

Prerequisites: `ffmpeg`, `curl`, Node.js 18+

```bash
cd ~/Develop/Pet/russian-app/content-prep
npm install
npm run prep
```

Expected output:
```
Wrote 4 units.
```

Verify:
- `../composeApp/src/commonMain/composeResources/files/content/manifest.json` exists
- `../composeApp/src/commonMain/composeResources/files/content/unit-*.json` exist
- `../composeApp/src/commonMain/composeResources/files/audio/*.m4a` exist
- `../composeApp/src/commonMain/composeResources/files/ATTRIBUTION.md` lists all clips

After regeneration, rebuild the app and commit the new content:
```bash
git add -A && git commit -m "feat: regenerate content from seed"
```

## Verifying that each clip says the right phrase

The pipeline guarantees the clips *exist* and are wired to the right card ids, but
not that a clip actually says the phrase on its card — a wrong Tatoeba id yields a
perfectly valid file of the wrong sentence, and no signal analysis can see it.

Two scripts settle this at the source, which is both stronger and cheaper than
listening to 110 clips. Run them in order after any `npm run prep` that changes
audio ids; both need network access.

```bash
node scripts/audio-provenance-check.mjs   # sentence id -> text -> attached audio id
node scripts/audio-source-match.mjs       # that recording -> the shipped .m4a
```

`audio-provenance-check.mjs` resolves every sentence id in `ATTRIBUTION.md` against
the Tatoeba API and checks the sentence is Russian, its text equals the card's
`russian` field, and the recorded audio id is attached to it. `audio-source-match.mjs`
then downloads each source recording and cross-correlates it against the bundled
`.m4a`. A lossy transcode of the same recording correlates at 0.99+; anything under
0.9 is a different waveform. Chained, they establish

```
card text == sentence text == recording of that sentence == shipped file
```

which is exactly what the by-ear pass was trying to confirm. Results land in
`scripts/audio-provenance.json` and `scripts/audio-source-match.json`; both are
committed as the durable record. Source downloads cache under `/tmp/slovo-asr/src`,
so re-runs are cheap.

### Optional cross-checks

`scripts/audio-asr-check.mjs` transcribes every clip with whisper.cpp and scores the
transcript against the card text. It needs `brew install whisper-cpp` plus a ggml
model, and it is *advisory only* — it misreads short clips, drops particles, and
writes numerals as digits, so a low score means "worth a look", never "wrong". The
source-match check supersedes it; it is kept because it validates the audio content
itself rather than the provenance metadata.

`scripts/audio-review.mjs` is the original human pass — it plays each clip next to
its Russian text:

```bash
node scripts/audio-review.mjs          # everything not yet judged
node scripts/audio-review.mjs greet    # only card ids starting with "greet"
node scripts/audio-review.mjs --all    # re-review, including judged cards
```

Keys: `enter`/`y` correct, `n` wrong, `r` replay, `s` skip, `q` quit. Verdicts are
written to `scripts/audio-review.json` after every keypress, so the pass can be
stopped and resumed. No longer required for release, but useful for judging clip
*quality* — clarity, pace, recording noise — which none of the automated checks
assess.
