#!/usr/bin/env node
// Machine half of the audio "does this clip say the right phrase?" check.
//
// Transcribes every bundled clip with whisper.cpp (Russian) and compares the
// transcript to the card's `russian` text. A wrong Tatoeba id produces a
// perfectly healthy audio file under a correct name, which no signal analysis
// catches — but the transcript will not match.
//
// This does NOT replace the by-ear pass in audio-review.mjs. ASR misreads
// short clips, drops particles and guesses at homophones, so a mismatch here
// means "a human should listen to this one", not "this clip is wrong". A
// match is strong evidence the clip is right.
//
//   node scripts/audio-asr-check.mjs                 # transcribe + compare all
//   node scripts/audio-asr-check.mjs --report        # re-compare cached transcripts
//
// Reads:  /tmp/slovo-asr/wav/<cardId>.wav  (16 kHz mono, see README)
// Writes: scripts/audio-asr.json           (transcript + score per card)

import { readFileSync, writeFileSync, existsSync, readdirSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const filesDir = join(root, "composeApp/src/commonMain/composeResources/files");
const wavDir = "/tmp/slovo-asr/wav";
const model = "/tmp/slovo-asr/models/ggml-large-v3-turbo.bin";
const outPath = join(root, "scripts/audio-asr.json");

const reportOnly = process.argv.includes("--report");

const cards = readdirSync(join(filesDir, "content"))
  .filter((f) => f.startsWith("unit-") && f.endsWith(".json"))
  .flatMap((f) => {
    const doc = JSON.parse(readFileSync(join(filesDir, "content", f), "utf8"));
    const lessonOf = new Map();
    for (const l of doc.lessons) for (const id of l.cardIds) lessonOf.set(id, l.title);
    return doc.cards.map((c) => ({ ...c, lesson: lessonOf.get(c.id) ?? "?" }));
  })
  .sort((a, b) => a.id.localeCompare(b.id));

// Whisper writes numbers as digits ("Уже 7.") where the cards spell them out
// ("Уже семь."). Identical speech, so both sides are folded to digits before
// comparing — otherwise every numeral card reads as a mismatch. Case forms are
// included because the cards decline them.
const NUMERALS = new Map(
  Object.entries({
    0: ["ноль"],
    1: ["один", "одна", "одно", "одну", "одного", "одной"],
    2: ["два", "две", "двух", "двум"],
    3: ["три", "трех", "трем"],
    4: ["четыре", "четырех", "четырем"],
    5: ["пять", "пяти"],
    6: ["шесть", "шести"],
    7: ["семь", "семи"],
    8: ["восемь", "восьми"],
    9: ["девять", "девяти"],
    10: ["десять", "десяти"],
    11: ["одиннадцать", "одиннадцати"],
    12: ["двенадцать", "двенадцати"],
    20: ["двадцать", "двадцати"],
    30: ["тридцать", "тридцати"],
    40: ["сорок", "сорока"],
    50: ["пятьдесят", "пятидесяти"],
    100: ["сто", "ста"],
  }).flatMap(([n, forms]) => forms.map((f) => [f, n])),
);

// Russian text is compared with case, punctuation and ё/е folded away — none of
// those are things the speaker can get wrong, and whisper is inconsistent about
// all three.
const normalise = (s) =>
  s
    .toLowerCase()
    .replace(/ё/g, "е")
    .replace(/\+/g, " плюс ")
    .replace(/=/g, " ") // the cards use a dash where whisper writes "="
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .split(/\s+/)
    .filter(Boolean)
    .map((w) => NUMERALS.get(w) ?? w)
    .join(" ")
    .trim();

const levenshtein = (a, b) => {
  if (a === b) return 0;
  if (!a.length) return b.length;
  if (!b.length) return a.length;
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  for (let i = 1; i <= a.length; i++) {
    const cur = [i];
    for (let j = 1; j <= b.length; j++) {
      cur[j] = Math.min(
        prev[j] + 1,
        cur[j - 1] + 1,
        prev[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1),
      );
    }
    prev = cur;
  }
  return prev[b.length];
};

const similarity = (a, b) => {
  const max = Math.max(a.length, b.length);
  return max === 0 ? 1 : 1 - levenshtein(a, b) / max;
};

const transcribe = (wav) => {
  const r = spawnSync(
    "whisper-cli",
    ["-m", model, "-l", "ru", "-nt", "-np", "--no-prints", "-f", wav],
    { encoding: "utf8", maxBuffer: 1 << 24 },
  );
  if (r.status !== 0) return { error: (r.stderr || "").trim().split("\n").slice(-3).join(" ") };
  return { text: (r.stdout || "").replace(/\s+/g, " ").trim() };
};

const cached = existsSync(outPath) ? JSON.parse(readFileSync(outPath, "utf8")) : {};
const results = {};

for (const [n, c] of cards.entries()) {
  const wav = join(wavDir, `${c.id}.wav`);
  let entry = reportOnly ? cached[c.id] : null;

  if (!entry) {
    if (!existsSync(wav)) {
      entry = { error: "wav missing — run the ffmpeg conversion first" };
    } else {
      process.stderr.write(`\r[${n + 1}/${cards.length}] ${c.id}${" ".repeat(20)}`);
      entry = transcribe(wav);
    }
  }

  const expected = normalise(c.russian);
  const heard = normalise(entry.text ?? "");
  results[c.id] = {
    ...entry,
    expected: c.russian,
    lesson: c.lesson,
    score: entry.error ? 0 : Number(similarity(expected, heard).toFixed(3)),
  };
}
process.stderr.write("\r" + " ".repeat(60) + "\r");

writeFileSync(outPath, JSON.stringify(results, null, 2) + "\n");

// Thresholds are deliberately loose: the goal is to shrink 110 clips down to a
// short list worth listening to, not to auto-fail anything.
const rows = Object.entries(results);
const clear = rows.filter(([, v]) => v.score >= 0.85);
const check = rows.filter(([, v]) => v.score >= 0.55 && v.score < 0.85);
const suspect = rows.filter(([, v]) => v.score < 0.55);

console.log(
  `${clear.length} clear (>=0.85), ${check.length} worth a listen (0.55-0.85), ` +
    `${suspect.length} suspect (<0.55) of ${rows.length}.\n`,
);

for (const [label, group] of [
  ["SUSPECT", suspect],
  ["WORTH A LISTEN", check],
]) {
  if (!group.length) continue;
  console.log(`== ${label} ==`);
  for (const [id, v] of group.sort((a, b) => a[1].score - b[1].score)) {
    console.log(`  ${v.score.toFixed(2)}  ${id}`);
    console.log(`        card:  ${v.expected}`);
    console.log(`        heard: ${v.error ? `<${v.error}>` : v.text}`);
  }
  console.log();
}
console.log(`Full output: ${outPath}`);
