#!/usr/bin/env bash
# Install HomePoint and the official Home on Fire companion on one TV with ADB debugging on.
# Optional third argument: a private folder (kept outside this repo) with favorites.txt and/or wallpapers/,
# copied into HomePoint's own folder on the TV.
set -euo pipefail
if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo "Usage: $0 <tv-address> <homepoint-release.apk> [private-folder]" >&2
  echo "Example: $0 192.168.1.23 homepoint-release.apk" >&2
  exit 1
fi
DEVICE="$1"
[[ "$DEVICE" == *:* ]] || DEVICE="$DEVICE:5555"
APK="$2"
PRIVATE="${3:-}"
PKG=com.aeropoint.homepoint
[[ -f "$APK" ]] || { echo "APK not found: $APK" >&2; exit 1; }
[[ -z "$PRIVATE" || -d "$PRIVATE" ]] || { echo "Private folder not found: $PRIVATE" >&2; exit 1; }
ADB="${ADB:-adb}"
command -v "$ADB" >/dev/null || { echo "adb not found: download Google's Platform-Tools (see README) and run with ADB=platform-tools/adb" >&2; exit 1; }
command -v curl >/dev/null || { echo "curl not found" >&2; exit 1; }
if command -v sha256sum >/dev/null; then SHA=(sha256sum); else SHA=(shasum -a 256); fi
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT
curl --fail --silent --show-error --location --proto '=https' --proto-redir '=https' \
  'https://github.com/toolicious/home-on-fire/releases/download/v1.5.2/home-on-fire.apk' \
  --output "$TEMP_DIR/home-on-fire.apk"
actual="$("${SHA[@]}" "$TEMP_DIR/home-on-fire.apk" | cut -d' ' -f1)"
[[ "$actual" == 87403c961264cc2e36e53cf571777ec20705eb87f46794cb0ac667135a98d9fe ]] \
  || { echo 'Home on Fire checksum mismatch; nothing installed' >&2; exit 1; }
out="$("$ADB" connect "$DEVICE" 2>&1 || true)"
echo "$out"
[[ "$out" == *"connected to"* ]] || { echo "Couldn't reach the TV at $DEVICE: check its address and that ADB debugging is on" >&2; exit 1; }
echo 'If the TV asks "Allow USB debugging?", tick "Always allow from this computer" and choose OK.'
"$ADB" -s "$DEVICE" wait-for-device
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
