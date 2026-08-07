#!/usr/bin/env bash
# Synthesizes the 6 Kosmo UI sound cues with ffmpeg (toy-synth-in-space per
# docs/design/design_handoff_slovo_app/SLOVO Visual Directions.dc.html §8a)
# and writes them straight into the bundled audio resources. Re-run after
# editing the tone parameters below to regenerate.
#
#   scripts/generate-sound-cues.sh

set -euo pipefail
cd "$(dirname "$0")/.."

OUT=composeApp/src/commonMain/composeResources/files/audio
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
cd "$TMP"

tone() { # freq duration fadeStart fadeDur vol out.wav
  ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=$1:duration=$2" \
    -af "afade=t=out:st=$3:d=$4:curve=exp,volume=$5" -ar 44100 -ac 1 "$6"
}

# 1. SELECT — dry woodblock pop, 60ms
tone 659.25 0.06 0 0.06 0.7 select.wav

# 2. SUCCESS — two-note marimba rise C5->E5, 250ms
tone 523.25 0.11 0.06 0.05 1.0 c5.wav
tone 659.25 0.14 0.07 0.07 1.0 e5.wav
ffmpeg -hide_banner -loglevel error -y -i c5.wav -i e5.wav -filter_complex "[0][1]concat=n=2:v=0:a=1,volume=0.7" success.wav

# 3. ERROR — soft felt womp, pitch down, 300ms
tone 392 0.15 0.08 0.07 1.0 g4.wav
tone 293.66 0.15 0.05 0.10 1.0 d4.wav
ffmpeg -hide_banner -loglevel error -y -i g4.wav -i d4.wav -filter_complex "[0][1]concat=n=2:v=0:a=1,volume=0.5" error.wav

# 4. PAIR_MATCH — click + sparkle tail, 180ms
tone 1046.5 0.02 0 0.02 0.6 click.wav
tone 2093 0.16 0 0.16 0.35 sparkle.wav
ffmpeg -hide_banner -loglevel error -y -i click.wav -i sparkle.wav -filter_complex "[0]apad=pad_dur=0.16[a];[a][1]amix=inputs=2:duration=longest" pair_match.wav

# 5. LESSON_COMPLETE — 4-note arpeggio C5-E5-G5-C6 + boop, 1.2s
tone 523.25 0.18 0.10 0.08 1.0 n1.wav
tone 659.25 0.18 0.10 0.08 1.0 n2.wav
tone 783.99 0.18 0.10 0.08 1.0 n3.wav
tone 1046.5 0.30 0.15 0.15 1.0 n4.wav
tone 220 0.35 0.15 0.20 0.6 boop.wav
ffmpeg -hide_banner -loglevel error -y -i n1.wav -i n2.wav -i n3.wav -i n4.wav -i boop.wav \
  -filter_complex "[0][1][2][3]concat=n=4:v=0:a=1[arp];[arp]apad=pad_dur=0.35[a];[a][4]amix=inputs=2:duration=longest,volume=0.8" lesson_complete.wav

# 6. STREAK_RESCUED — rising sweep into a warm chord, 900ms
ffmpeg -hide_banner -loglevel error -y -f lavfi -i "aevalsrc=0.25*sin(2*PI*(300+900*t)*t):duration=0.4" \
  -af "afade=t=in:st=0:d=0.35,afade=t=out:st=0.3:d=0.1" whoosh.wav
ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=261.63:duration=0.55" -af "afade=t=in:st=0:d=0.08,afade=t=out:st=0.35:d=0.20,volume=0.5" chord_c.wav
ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=329.63:duration=0.55" -af "afade=t=in:st=0:d=0.08,afade=t=out:st=0.35:d=0.20,volume=0.4" chord_e.wav
ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=392.00:duration=0.55" -af "afade=t=in:st=0:d=0.08,afade=t=out:st=0.35:d=0.20,volume=0.4" chord_g.wav
ffmpeg -hide_banner -loglevel error -y -i whoosh.wav -i chord_c.wav -i chord_e.wav -i chord_g.wav \
  -filter_complex "[1][2][3]amix=inputs=3:duration=longest[chord];[chord]adelay=350|350[chorddelay];[0]apad=pad_dur=0.55[w];[w][chorddelay]amix=inputs=2:duration=longest,volume=1.3" streak_rescued.wav

mkdir -p "$OLDPWD/$OUT"
for pair in select:cue-select success:cue-success error:cue-error \
            pair_match:cue-pair-match lesson_complete:cue-lesson-complete streak_rescued:cue-streak-rescued; do
  src="${pair%%:*}"; dst="${pair##*:}"
  ffmpeg -hide_banner -loglevel error -y -i "$src.wav" -ar 44100 -ac 1 -c:a aac -b:a 96k "$OLDPWD/$OUT/$dst.m4a"
done

echo "Wrote 6 cue assets to $OUT/"
