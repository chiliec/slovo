#!/usr/bin/env node
// Verifies every bundled clip against its source sentence on Tatoeba.
//
// The failure mode this exists for: a wrong Tatoeba id yields a perfectly
// healthy audio file, correctly named, that says the wrong phrase. Signal
// analysis cannot see it. This checks it at the source instead of by ear —
// for each clip it confirms that
//
//   1. the sentence id in ATTRIBUTION.md still resolves,
//   2. its text equals the card's `russian` text,
//   3. the recorded audio id is actually attached to that sentence,
//   4. the sentence is in Russian.
//
// If all four hold, the clip is the recording of that sentence, and the
// sentence is the card's phrase. Requires network access.
//
//   node scripts/audio-provenance-check.mjs
//
// Writes scripts/audio-provenance.json.

import { readFileSync, writeFileSync, readdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const filesDir = join(root, "composeApp/src/commonMain/composeResources/files");
const outPath = join(root, "scripts/audio-provenance.json");

const cards = new Map(
  readdirSync(join(filesDir, "content"))
    .filter((f) => f.startsWith("unit-") && f.endsWith(".json"))
    .flatMap((f) => JSON.parse(readFileSync(join(filesDir, "content", f), "utf8")).cards)
    .map((c) => [c.id, c]),
);

// - <cardId>.m4a — Tatoeba sentence #374656 (audio #29575) by Inego — CC-BY — <url>
const attribution = readFileSync(join(filesDir, "ATTRIBUTION.md"), "utf8");
const entries = [...attribution.matchAll(/^- (\S+)\.m4a — Tatoeba sentence #(\d+) \(audio #(\d+)\)/gm)].map(
  ([, id, sentenceId, audioId]) => ({ id, sentenceId, audioId }),
);

// Punctuation and ё/е are editorial, not spoken; everything else must agree.
const normalise = (s) =>
  s
    .toLowerCase()
    .replace(/ё/g, "е")
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .replace(/\s+/g, " ")
    .trim();

const results = {};
let ok = 0;
const problems = [];

for (const [n, e] of entries.entries()) {
  process.stderr.write(`\r[${n + 1}/${entries.length}] ${e.id}${" ".repeat(20)}`);
  const card = cards.get(e.id);
  const record = (status, detail) => {
    results[e.id] = { status, detail, ...e, card: card?.russian };
    if (status === "ok") ok += 1;
    else problems.push({ id: e.id, status, detail });
  };

  if (!card) {
    record("no-card", "attribution entry has no matching card");
    continue;
  }

  // Tatoeba drops the occasional connection over a run this long; a bare
  // failure here would look identical to a real provenance problem.
  let doc, lastErr;
  for (let attempt = 0; attempt < 3 && !doc; attempt++) {
    if (attempt) await new Promise((r) => setTimeout(r, 1000 * attempt));
    try {
      const res = await fetch(`https://tatoeba.org/en/api_v0/sentence/${e.sentenceId}`, {
        signal: AbortSignal.timeout(20000),
      });
      if (!res.ok) {
        lastErr = `HTTP ${res.status}`;
        continue;
      }
      doc = await res.json();
    } catch (err) {
      lastErr = String(err);
    }
  }
  if (!doc) {
    record("fetch-failed", lastErr);
    continue;
  }

  const remote = doc?.text ?? "";
  results[e.id] = { ...e, card: card.russian, remote, lang: doc?.lang };

  if (doc?.lang !== "rus") record("wrong-lang", `sentence lang is ${doc?.lang}`);
  else if (normalise(remote) !== normalise(card.russian))
    record("text-mismatch", `card "${card.russian}" vs sentence "${remote}"`);
  else if (!(doc.audios ?? []).some((a) => String(a.id) === e.audioId))
    record("audio-not-attached", `audio #${e.audioId} not listed on sentence #${e.sentenceId}`);
  else record("ok", null);

  await new Promise((r) => setTimeout(r, 250)); // be polite to Tatoeba
}
process.stderr.write("\r" + " ".repeat(60) + "\r");

writeFileSync(outPath, JSON.stringify(results, null, 2) + "\n");

console.log(`${ok} verified at source, ${problems.length} problem(s) of ${entries.length}.\n`);
for (const p of problems) console.log(`  ${p.status.padEnd(18)} ${p.id}  ${p.detail}`);
console.log(`\nFull output: ${outPath}`);
