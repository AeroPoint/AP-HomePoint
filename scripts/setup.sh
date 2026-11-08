#!/usr/bin/env bash
# Install HomePoint and the official Home on Fire companion on one TV with ADB debugging on.
# Optional third argument: a private folder (kept outside this repo) with favorites.txt and/or wallpapers/,
# copied into HomePoint's own folder on the TV.
set -euo pipefail
if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo "Usage: $0 <fire-tv-ip:5555> <homepoint-release.apk> [private-folder]" >&2
  exit 1
fi
DEVICE="$1"
APK="$2"
PRIVATE="${3:-}"
PKG=com.aeropoint.homepoint
[[ -f "$APK" ]] || { echo "APK not found: $APK" >&2; exit 1; }
[[ -z "$PRIVATE" || -d "$PRIVATE" ]] || { echo "Private folder not found: $PRIVATE" >&2; exit 1; }
ADB="${ADB:-adb}"
command -v "$ADB" >/dev/null
command -v curl >/dev/null
command -v python3 >/dev/null
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT
curl --fail --location --proto '=https' --proto-redir '=https' \
  'https://github.com/toolicious/home-on-fire/releases/download/v1.5.2/home-on-fire.apk' \
  --output "$TEMP_DIR/home-on-fire.apk"
python3 - "$TEMP_DIR/home-on-fire.apk" <<'PY'
import hashlib, pathlib, sys
actual = hashlib.sha256(pathlib.Path(sys.argv[1]).read_bytes()).hexdigest()
if actual != '87403c961264cc2e36e53cf571777ec20705eb87f46794cb0ac667135a98d9fe':
    sys.exit('Home on Fire checksum mismatch; nothing installed')
PY
"$ADB" connect "$DEVICE"
"$ADB" -s "$DEVICE" get-state
"$ADB" -s "$DEVICE" install -r "$APK"
"$ADB" -s "$DEVICE" install -r "$TEMP_DIR/home-on-fire.apk"
"$ADB" -s "$DEVICE" shell pm grant io.github.toolicious.homeonfire android.permission.WRITE_SECURE_SETTINGS
# The background photo comes from the shared Pictures/HomePoint folder: allow HomePoint to read photos
# (READ_EXTERNAL_STORAGE up to Android 12, READ_MEDIA_IMAGES from 13; the other one fails harmlessly).
"$ADB" -s "$DEVICE" shell pm grant "$PKG" android.permission.READ_EXTERNAL_STORAGE 2>/dev/null || true
"$ADB" -s "$DEVICE" shell pm grant "$PKG" android.permission.READ_MEDIA_IMAGES 2>/dev/null || true
if [[ -n "$PRIVATE" ]]; then
  if [[ -d "$PRIVATE/wallpapers" ]]; then
    "$ADB" -s "$DEVICE" shell mkdir -p /sdcard/Pictures/HomePoint
    for f in "$PRIVATE"/wallpapers/*; do [[ -f "$f" ]] && "$ADB" -s "$DEVICE" push "$f" /sdcard/Pictures/HomePoint/; done
  fi
  if [[ -f "$PRIVATE/favorites.txt" ]]; then
    # Only where the device lets adb write into the app's own folder (Fire OS 8 doesn't): there, arrange on the TV.
    "$ADB" -s "$DEVICE" shell monkey -p "$PKG" -c android.intent.category.LEANBACK_LAUNCHER 1 >/dev/null
    sleep 3
    "$ADB" -s "$DEVICE" push "$PRIVATE/favorites.txt" "/sdcard/Android/data/$PKG/files/favorites.txt" 2>/dev/null \
      || echo "favorites.txt not copied (this TV blocks app folders): add favorites on the TV (hold OK on an app)"
  fi
fi
"$ADB" -s "$DEVICE" shell am start -n io.github.toolicious.homeonfire/.MainActivity
echo 'On the TV: in Home on Fire choose HomePoint as the target, turn on Replace Home (and Launch on boot).'
echo 'Disable other launcher accessibility redirectors first. Hold Home to reach Amazon home.'
echo 'Then turn ADB debugging off again: Settings > My Fire TV > Developer options.'
