// Generates all app-store launcher icons for SLOVO from the in-app MishaMascot
// bear geometry (see composeApp/.../ui/components/Mascot.kt). Single source of
// truth for the brand mark: change the bear here and re-run `npm run icons`.
//
//   node gen-icons.mjs        (from content-prep/)
//
// Outputs Android mipmaps, the iOS AppIcon asset, and store-upload PNGs.
import sharp from "sharp";
import { mkdir, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO = resolve(HERE, "..");

// --- Slovo palette (mirror of ui/theme/Color.kt) ---
const YELLOW = "#FFCE1F";
const BEAR = "#C98A4B";
const BEAR_LT = "#E8C79B";
const INK = "#141414";

// The bear drawn in a 100x100 logical space, matching Mascot.kt exactly.
// Draw order: ears, head (covers ear bottoms), muzzle, eyes, nose.
const BEAR_100 = `
  <circle cx="26" cy="24" r="11" fill="${BEAR}" stroke="${INK}" stroke-width="4"/>
  <circle cx="74" cy="24" r="11" fill="${BEAR}" stroke="${INK}" stroke-width="4"/>
  <rect x="18" y="18" width="64" height="64" rx="18" ry="18" fill="${BEAR}" stroke="${INK}" stroke-width="4"/>
  <rect x="36" y="52" width="28" height="20" rx="10" ry="10" fill="${BEAR_LT}" stroke="${INK}" stroke-width="3.5"/>
  <rect x="33" y="38" width="8" height="8" fill="${INK}"/>
  <rect x="59" y="38" width="8" height="8" fill="${INK}"/>
  <rect x="45" y="54" width="10" height="8" rx="2" ry="2" fill="${INK}"/>
`;

// The bear's visual centre in its own 100-space (symmetric horizontally; the
// ears+head+muzzle bbox centres vertically around y=47.5).
const BEAR_CX = 50;
const BEAR_CY = 47.5;

// Compose an icon SVG at S px. `frac` = fraction of the canvas the bear spans.
// mode: "full" (yellow field), "foreground" (transparent, for adaptive), "round".
function iconSvg(S, frac, mode) {
  const k = (frac * S) / 100;
  const tx = S / 2 - BEAR_CX * k;
  const ty = S / 2 - BEAR_CY * k;
  const bg =
    mode === "foreground"
      ? ""
      : `<rect width="${S}" height="${S}" fill="${YELLOW}"${
          mode === "round" ? ` clip-path="url(#c)"` : ""
        }/>`;
  const clip =
    mode === "round"
      ? `<defs><clipPath id="c"><circle cx="${S / 2}" cy="${S / 2}" r="${
          S / 2
        }"/></clipPath></defs>`
      : "";
  const group =
    mode === "round"
      ? `<g clip-path="url(#c)"><g transform="translate(${tx} ${ty}) scale(${k})">${BEAR_100}</g></g>`
      : `<g transform="translate(${tx} ${ty}) scale(${k})">${BEAR_100}</g>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${S}" height="${S}" viewBox="0 0 ${S} ${S}">${clip}${bg}${group}</svg>`;
}

async function png(path, svg, { flatten = false } = {}) {
  const abs = resolve(REPO, path);
  await mkdir(dirname(abs), { recursive: true });
  let img = sharp(Buffer.from(svg));
  if (flatten) img = img.flatten({ background: YELLOW }).removeAlpha(); // iOS: no alpha channel allowed
  await img.png().toFile(abs);
  console.log("  " + path);
}

const AND = "composeApp/src/androidMain/res";
// density -> [legacy launcher px, adaptive foreground px]
const DENSITIES = {
  mdpi: [48, 108],
  hdpi: [72, 162],
  xhdpi: [96, 216],
  xxhdpi: [144, 324],
  xxxhdpi: [192, 432],
};

console.log("Android launcher icons:");
for (const [d, [legacy, fg]] of Object.entries(DENSITIES)) {
  await png(`${AND}/mipmap-${d}/ic_launcher.png`, iconSvg(legacy, 0.66, "full"));
  await png(`${AND}/mipmap-${d}/ic_launcher_round.png`, iconSvg(legacy, 0.66, "round"));
  await png(`${AND}/mipmap-${d}/ic_launcher_foreground.png`, iconSvg(fg, 0.56, "foreground"));
}

console.log("iOS AppIcon:");
await png("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon-1024.png", iconSvg(1024, 0.66, "full"), { flatten: true });

console.log("Store-upload icons:");
await png("store-assets/icons/play-icon-512.png", iconSvg(512, 0.66, "full"), { flatten: true });
await png("store-assets/icons/appstore-icon-1024.png", iconSvg(1024, 0.66, "full"), { flatten: true });

// A crisp master SVG for future editing / vector uploads.
await writeFile(resolve(REPO, "store-assets/icons/slovo-icon.svg"), iconSvg(1024, 0.66, "full"));
console.log("  store-assets/icons/slovo-icon.svg");

// Google Play feature graphic: 1024x500, sand field, bear + SLOVO wordmark,
// framed with the neo-brutalist hard ink border.
console.log("Play feature graphic:");
const FG_W = 1024, FG_H = 500;
const bk = (0.62 * FG_H) / 100; // bear scaled to ~62% of the graphic height
const btx = 250 - BEAR_CX * bk; // bear centred around x=250
const bty = FG_H / 2 - BEAR_CY * bk;
const featureSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="${FG_W}" height="${FG_H}" viewBox="0 0 ${FG_W} ${FG_H}">
  <rect width="${FG_W}" height="${FG_H}" fill="${"#F3EEE2"}"/>
  <rect x="14" y="14" width="${FG_W - 28}" height="${FG_H - 28}" fill="none" stroke="${INK}" stroke-width="14"/>
  <g transform="translate(${btx} ${bty}) scale(${bk})">${BEAR_100}</g>
  <text x="468" y="250" font-family="Arial Black, Helvetica, Arial, sans-serif" font-weight="900" font-size="120" fill="${INK}">SLOVO</text>
  <text x="472" y="316" font-family="Helvetica, Arial, sans-serif" font-weight="700" font-size="40" fill="${"#E8402A"}">Russian phrases</text>
</svg>`;
await png("store-assets/feature-graphic/play-feature-1024x500.png", featureSvg, { flatten: true });
await writeFile(resolve(REPO, "store-assets/feature-graphic/play-feature.svg"), featureSvg);
console.log("  store-assets/feature-graphic/play-feature.svg");

console.log("Done.");
