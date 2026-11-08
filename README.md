# HomePoint

A calm home screen for Fire TV and Android TV: your favorite apps in a large-artwork row, the apps you used
recently, then everything else, over a photo of your choice. No ads, no sponsored rows, and no internet use at all
(the app has no INTERNET permission).

Fork of [HomeScreen by Arash Vakil](https://github.com/arashvakil/HomeScreen) (MIT, see [LICENSE](LICENSE)).

## Changes from HomeScreen
- Removed: weather, favorites sync between TVs, and the self-updater, with the internet, install and boot
  permissions and the networking libraries they needed.
- Added: a "Recently used" row (kept on the TV, skips favorites), a Live TV tile for the TV's antenna tuner, a
  photo background, first-run favorites from a private file, and arranging that ignores uninstalled favorites.
- CI builds unsigned and signs release builds in a final step; pull requests never see the key.

## Install on a Fire TV
1. On the TV: Settings → My Fire TV → About, click the TV's name 7 times; then Developer options → ADB debugging on.
2. On a computer with `adb`: `scripts/setup.sh <tv-ip>:5555 homepoint-release.apk [private-folder]`. It installs
   HomePoint and the Home on Fire companion (download pinned by checksum), lets HomePoint read photos, and, if you
   pass a private folder, copies its `wallpapers/` to the TV's `Pictures/HomePoint` folder.
3. On the TV: in Home on Fire, pick HomePoint as the target and turn on Replace Home (and Launch on boot).
4. Turn ADB debugging off again.

Hold Home for Amazon's home screen. To undo, turn off Home on Fire's redirect or uninstall both apps.

### Private files (not in this repo)
- `wallpapers/`: pictures for the background, copied to `/sdcard/Pictures/HomePoint/`; the first by file name is shown.
- `favorites.txt`: one package name per line, used the first time HomePoint starts, on devices that let adb write
  into an app's own folder. Fire OS 8 doesn't: there, add favorites on the TV (hold OK on an app).

## License
MIT, as upstream. Home on Fire is a separate app under GPL-3.0 and is not included.
