#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/metadata/android/en-US/images/phoneScreenshots"
DEVICE_DIR="/sdcard/Pictures/StutterScreenshots"

# Ensure output directory exists
mkdir -p "$OUT_DIR"

# Run the instrumentation test
./scripts/gradlew-java17.sh --no-daemon connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.tomasino.stutter.ScreenshotCaptureTest

# Pull PNGs from device
adb pull "$DEVICE_DIR" "$OUT_DIR" >/dev/null

# Flatten if adb created a nested directory
if [ -d "$OUT_DIR/StutterScreenshots" ]; then
  mv "$OUT_DIR/StutterScreenshots/"*.png "$OUT_DIR"/
  rmdir "$OUT_DIR/StutterScreenshots"
fi

# Map expected file names
MAP=(
  "1_light_home.png 1.jpg"
  "2_light_play.png 2.jpg"
  "3_dark_home.png 3.jpg"
  "4_dark_play.png 4.jpg"
)

# Convert to JPG and overwrite
for entry in "${MAP[@]}"; do
  src="$OUT_DIR/$(echo "$entry" | awk '{print $1}')"
  dst="$OUT_DIR/$(echo "$entry" | awk '{print $2}')"
  if [ -f "$src" ]; then
    convert "$src" -quality 90 "$dst"
  else
    echo "WARN: missing $src"
  fi
done

# Cleanup intermediate PNGs
rm -f "$OUT_DIR"/*.png

# List final files
ls -lh "$OUT_DIR"/1.jpg "$OUT_DIR"/2.jpg "$OUT_DIR"/3.jpg "$OUT_DIR"/4.jpg
