#!/usr/bin/env node
// Proves each bundled clip IS the Tatoeba recording it claims to be.
//
// audio-provenance-check.mjs establishes that the sentence id in
// ATTRIBUTION.md holds the card's phrase and that the recorded audio id is
// attached to it. That still leaves one link unproven: whether the .m4a we
// actually ship is that recording, or something that went astray during
// download/convert. This closes it by downloading the source audio and
// cross-correlating the waveforms.
//
// Together the two scripts answer the "does this clip say the right phrase?"
// question end to end, without needing ears:
//
//   card text == sentence text == recording of that sentence == shipped file
//
// Requires network access and ffmpeg. Source downloads are cached under
// /tmp/slovo-asr/src, so re-runs are cheap.
//
//   node scripts/audio-source-match.mjs
//
// Writes scripts/audio-source-match.json.

import { readFileSync, writeFileSync, existsSync, mkdirSync, statSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const audioDir = join(root, "composeApp/src/commonMain/composeResources/files/audio");
const attributionPath = join(
  root,
  "composeApp/src/commonMain/composeResources/files/ATTRIBUTION.md",
);
const cacheDir = "/tmp/slovo-asr/src";
const outPath = join(root, "scripts/audio-source-match.json");

const SR = 16000;
const MAX_LAG = Math.round(0.15 * SR); // AAC padding is ~ms, 150ms is generous

mkdirSync(cacheDir, { recursive: true });

const entries = [
  ...readFileSync(attributionPath, "utf8").matchAll(
    /^- (\S+)\.m4a — Tatoeba sentence #(\d+) \(audio #(\d+)\)/gm,
  ),
].map(([, id, sentenceId, audioId]) => ({ id, sentenceId, audioId }));

// Decode anything ffmpeg understands into mono 16 kHz signed 16-bit samples.
const decode = (path) => {
  const r = spawnSync(
    "ffmpeg",
    ["-nostdin", "-loglevel", "error", "-i", path, "-ar", String(SR), "-ac", "1", "-f", "s16le", "-"],
    { maxBuffer: 1 << 28 },
  );
  if (r.status !== 0) return null;
  const buf = r.stdout;
  const out = new Float64Array(buf.length >> 1);
  for (let i = 0; i < out.length; i++) out[i] = buf.readInt16LE(i * 2);
  return out;
};

const correlateAt = (a, b, lag) => {
  const aOff = Math.max(0, lag);
  const bOff = Math.max(0, -lag);
  const n = Math.min(a.length - aOff, b.length - bOff);
  if (n < SR / 4) return -2;
  let dot = 0,
    sa = 0,
    sb = 0;
  for (let i = 0; i < n; i++) {
    const x = a[aOff + i],
      y = b[bOff + i];
    dot += x * y;
    sa += x * x;
    sb += y * y;
  }
  return sa && sb ? dot / Math.sqrt(sa * sb) : -2;
};

// Coarse sweep then refine — a full sample-by-sample sweep is needlessly slow.
const bestCorrelation = (a, b) => {
  let best = { r: -2, lag: 0 };
  for (let lag = -MAX_LAG; lag <= MAX_LAG; lag += 16) {
    const r = correlateAt(a, b, lag);
    if (r > best.r) best = { r, lag };
  }
  const centre = best.lag;
  for (let lag = centre - 16; lag <= centre + 16; lag++) {
    const r = correlateAt(a, b, lag);
    if (r > best.r) best = { r, lag };
  }
  return best;
};

const results = {};
const problems = [];
let matched = 0;

for (const [n, e] of entries.entries()) {
  process.stderr.write(`\r[${n + 1}/${entries.length}] ${e.id}${" ".repeat(20)}`);
  const record = (status, detail, extra = {}) => {
    results[e.id] = { status, detail, ...e, ...extra };
    if (status === "match") matched += 1;
    else problems.push({ id: e.id, status, detail });
  };

  const srcPath = join(cacheDir, `${e.audioId}.mp3`);
  if (!existsSync(srcPath) || statSync(srcPath).size === 0) {
    const dl = spawnSync("curl", [
      "-sL", "--fail", "--max-time", "60", "--retry", "3", "--retry-delay", "1",
      "-o", srcPath,
      `https://tatoeba.org/en/audio/download/${e.audioId}`,
    ]);
    if (dl.status !== 0) {
      record("download-failed", `curl exit ${dl.status} for audio #${e.audioId}`);
      continue;
    }
    await new Promise((r) => setTimeout(r, 200)); // be polite to Tatoeba
  }

  const bundled = decode(join(audioDir, `${e.id}.m4a`));
  const source = decode(srcPath);
  if (!bundled || !source) {
    record("decode-failed", !bundled ? "bundled clip did not decode" : "source audio did not decode");
    continue;
  }

  const { r, lag } = bestCorrelation(bundled, source);
  const extra = {
    correlation: Number(r.toFixed(4)),
    lagMs: Number(((lag / SR) * 1000).toFixed(1)),
    bundledSec: Number((bundled.length / SR).toFixed(2)),
    sourceSec: Number((source.length / SR).toFixed(2)),
  };
  // Lossy transcode of an identical recording lands at 0.99+; anything below
  // 0.9 is a different waveform, not a codec artifact.
  if (r >= 0.9) record("match", null, extra);
  else record("waveform-differs", `peak correlation ${r.toFixed(3)}`, extra);
}
process.stderr.write("\r" + " ".repeat(60) + "\r");

writeFileSync(outPath, JSON.stringify(results, null, 2) + "\n");

const rs = Object.values(results).filter((v) => v.correlation != null).map((v) => v.correlation);
console.log(`${matched} of ${entries.length} bundled clips match their Tatoeba source recording.`);
if (rs.length) {
  console.log(`correlation: min ${Math.min(...rs).toFixed(4)}, max ${Math.max(...rs).toFixed(4)}`);
}
if (problems.length) {
  console.log(`\n${problems.length} problem(s):`);
  for (const p of problems) console.log(`  ${p.status.padEnd(18)} ${p.id}  ${p.detail}`);
}
console.log(`\nFull output: ${outPath}`);
