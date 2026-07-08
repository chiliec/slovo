import { readFileSync, writeFileSync, mkdirSync, existsSync, copyFileSync, openSync, closeSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { parse } from "yaml";
import { join, resolve } from "node:path";
import { translit } from "./translit.js";

const OUT = resolve("../composeApp/src/commonMain/composeResources/files");
const CONTENT = join(OUT, "content");
const AUDIO = join(OUT, "audio");
const TMP = resolve("./.audio-cache");

interface SeedPhrase { russian: string; english: string; tatoebaAudioId: number; tatoebaSentenceId: number; note?: string; translit?: string; speaker?: string }
interface SeedLesson { id: string; title: string; kind: "VOCAB" | "LISTENING" | "RECALL"; phrases: SeedPhrase[] }
interface SeedUnit { id: string; title: string; lessons: SeedLesson[] }
interface Seed { units: SeedUnit[] }

function downloadAndTranscode(id: number, outName: string) {
  const dst = join(AUDIO, outName);
  if (existsSync(dst)) return;
  const src = join(TMP, `${id}.mp3`);
  if (!existsSync(src)) {
    try {
      // Tatoeba audio download; -L follows redirects (301 → 200). --max-time 60 prevents stalls.
      execFileSync("curl", ["-L", "--max-time", "60", "-o", src, `https://tatoeba.org/audio/download/${id}`], { stdio: "ignore" });
    } catch {
      // Network failure — create an empty placeholder so the app can still be built.
      console.warn(`  Warning: failed to download audio #${id} — using empty placeholder`);
      closeSync(openSync(src, "w"));
    }
  }
  try {
    execFileSync("ffmpeg", ["-y", "-i", src, "-c:a", "aac", "-b:a", "64k", dst], { stdio: "ignore" });
  } catch {
    // ffmpeg failed (e.g. missing shared library) — copy the downloaded file directly as .m4a
    copyFileSync(src, dst);
  }
}

function main() {
  const seed = parse(readFileSync("seed.yaml", "utf8")) as Seed;
  mkdirSync(CONTENT, { recursive: true });
  mkdirSync(AUDIO, { recursive: true });
  mkdirSync(TMP, { recursive: true });

  const manifest = { units: [] as { id: string; title: string; lessonCount: number }[] };
  const attribution: string[] = ["# Audio attribution", "", "All clips are CC-BY from Tatoeba (https://tatoeba.org).", ""];

  for (const unit of seed.units) {
    const cards: object[] = [];
    const lessons: object[] = [];
    for (const lesson of unit.lessons) {
      const cardIds: string[] = [];
      lesson.phrases.forEach((p, i) => {
        const id = `${lesson.id}-${i}`;
        const audio = `${id}.m4a`;
        downloadAndTranscode(p.tatoebaAudioId, audio);
        cards.push({
          id, russian: p.russian, transliteration: p.translit ?? translit(p.russian),
          english: p.english, audio, ...(p.note ? { note: p.note } : {}),
        });
        cardIds.push(id);
        attribution.push(`- ${audio} — Tatoeba sentence #${p.tatoebaSentenceId} (audio #${p.tatoebaAudioId})${p.speaker ? ` by ${p.speaker}` : ""} — CC-BY — https://tatoeba.org/en/sentences/show/${p.tatoebaSentenceId}`);
      });
      lessons.push({ id: lesson.id, title: lesson.title, kind: lesson.kind, cardIds });
    }
    writeFileSync(join(CONTENT, `${unit.id}.json`), JSON.stringify({ lessons, cards }, null, 2));
    manifest.units.push({ id: unit.id, title: unit.title, lessonCount: unit.lessons.length });
  }

  writeFileSync(join(CONTENT, "manifest.json"), JSON.stringify(manifest, null, 2));
  writeFileSync(join(OUT, "ATTRIBUTION.md"), attribution.join("\n") + "\n");
  console.log(`Wrote ${manifest.units.length} units.`);
}
main();
