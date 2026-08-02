#!/usr/bin/env node
// Interactive by-ear review of the bundled audio clips.
//
// Plays each clip via `afplay` and shows the Russian text it is supposed to
// say, so mislabelled or wrong-phrase recordings can be caught without opening
// the app. Verdicts are appended to scripts/audio-review.json, so the review can
// be stopped and resumed.
//
//   node scripts/audio-review.mjs            # review everything not yet judged
//   node scripts/audio-review.mjs greet      # only cards whose id starts with "greet"
//   node scripts/audio-review.mjs --all      # re-review, including already-judged cards
//
// Keys: Enter/y = correct, n = wrong, r = replay, s = skip, q = save and quit.

import { readFileSync, writeFileSync, existsSync, readdirSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import readline from "node:readline";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const filesDir = join(root, "composeApp/src/commonMain/composeResources/files");
const audioDir = join(filesDir, "audio");
const resultsPath = join(root, "scripts/audio-review.json");

const args = process.argv.slice(2);
const reviewAll = args.includes("--all");
const filter = args.find((a) => !a.startsWith("--"));

const cards = readdirSync(join(filesDir, "content"))
  .filter((f) => f.startsWith("unit-") && f.endsWith(".json"))
  .flatMap((f) => {
    const unit = f.replace(/^unit-|\.json$/g, "");
    const doc = JSON.parse(readFileSync(join(filesDir, "content", f), "utf8"));
    const lessonOf = new Map();
    for (const l of doc.lessons) for (const id of l.cardIds) lessonOf.set(id, l.title);
    return doc.cards.map((c) => ({ ...c, unit, lesson: lessonOf.get(c.id) ?? "?" }));
  });

const results = existsSync(resultsPath) ? JSON.parse(readFileSync(resultsPath, "utf8")) : {};

const queue = cards.filter(
  (c) => (!filter || c.id.startsWith(filter)) && (reviewAll || !results[c.id]),
);

if (queue.length === 0) {
  console.log("Nothing to review. Pass --all to re-review judged cards.");
  process.exit(0);
}

const play = (file) => spawnSync("afplay", [join(audioDir, file)], { stdio: "ignore" });

const save = () => writeFileSync(resultsPath, JSON.stringify(results, null, 2) + "\n");

const summarise = () => {
  const judged = Object.values(results);
  const wrong = Object.entries(results).filter(([, v]) => v.verdict === "wrong");
  console.log(
    `\n${judged.filter((v) => v.verdict === "ok").length} ok, ${wrong.length} wrong, ` +
      `${judged.filter((v) => v.verdict === "skip").length} skipped, ` +
      `${cards.length - judged.length} unreviewed of ${cards.length}.`,
  );
  if (wrong.length) {
    console.log("\nFlagged as wrong:");
    for (const [id, v] of wrong) console.log(`  ${id}  ${v.russian}`);
  }
  console.log(`\nSaved to ${resultsPath}`);
};

readline.emitKeypressEvents(process.stdin);
if (process.stdin.isTTY) process.stdin.setRawMode(true);

let i = 0;
const show = () => {
  const c = queue[i];
  console.log(
    `\n[${i + 1}/${queue.length}] ${c.unit} / ${c.lesson}\n` +
      `  ${c.russian}\n  ${c.transliteration}\n  ${c.english}\n  ${c.audio}`,
  );
  play(c.audio);
  prompt();
};

const finish = (code = 0) => {
  save();
  summarise();
  process.exit(code);
};

const prompt = () => process.stdout.write("  [enter=ok  n=wrong  r=replay  s=skip  q=quit] ");

process.stdin.on("keypress", (_str, key) => {
  if (key.ctrl && key.name === "c") finish(1);
  const c = queue[i];
  const record = (verdict) => {
    results[c.id] = { verdict, russian: c.russian, audio: c.audio };
    save();
    i += 1;
    if (i >= queue.length) finish();
    else show();
  };
  switch (key.name) {
    case "return":
    case "y":
      console.log("ok");
      return record("ok");
    case "n":
      console.log("WRONG");
      return record("wrong");
    case "s":
      console.log("skipped");
      return record("skip");
    case "r":
      console.log("replay");
      play(c.audio);
      return prompt();
    case "q":
      return finish();
  }
});

console.log(`Reviewing ${queue.length} clip(s). Verdicts save after every key.`);
show();
