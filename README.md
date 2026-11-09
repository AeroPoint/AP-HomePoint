# HomePoint

A calm home screen for Fire TV and Android TV. Home shows your six favorite apps as big frosted-glass tiles,
three across and two rows, each with a simple white logo, over a photo of your choice. All Apps lists the apps you
used recently, then everything else in groups (Streaming, Live TV, Kids, Music, and so on). No ads, no sponsored rows,
and no internet use at all (the app has no INTERNET permission).

Fork of [HomeScreen by Arash Vakil](https://github.com/arashvakil/HomeScreen) (MIT, see [LICENSE](LICENSE)).

## Changes from HomeScreen
- Removed: weather, favorites sync between TVs, and the self-updater, with the internet, install and boot
  permissions and the networking libraries they needed.
- Added: the 3x2 tile Home with white logos made from each app's own artwork, All Apps in groups with a "Recently
  used" group first (kept on the TV, skips favorites), a Live TV tile for the TV's antenna tuner, a photo background,
  first-run favorites from a private file, and arranging that ignores uninstalled favorites.
- CI builds unsigned and signs release builds in a final step; pull requests never see the key.

## Install on a Fire TV
You need the TV and a computer (Mac or Windows) on the same Wi-Fi. It takes about 15 minutes.

1. **Turn on ADB debugging on the TV.** Settings → My Fire TV (on some TVs: Device & Software) → About. Click the
   TV's name 7 times until it says you're a developer. Go back one screen → Developer options → turn on
   **ADB debugging**.
2. **Install from the computer.**
   1. **Find the TV's address.** On the TV: Settings → My Fire TV (or Device & Software) → About → Network. Write
      down the **IP Address**, for example `192.168.1.23`.
   2. **Download three things** and unzip each one:
      - This project: at the top of this page, the green **Code** button → **Download ZIP**. You get a folder called
        `AP-HomePoint-main`.
      - The HomePoint app: the **Actions** tab at the top of this page → the newest run with a green check mark →
        scroll down to **Artifacts** → **homepoint-apks** (GitHub asks you to sign in). After unzipping, the file you
        need is `homepoint-release.apk`, inside the folders app → build → outputs → apk → release.
      - Google's ADB tool: [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) →
        the download for Mac or Windows. You get a folder called `platform-tools`.
   3. **Put them together.** Move `homepoint-release.apk` and the `platform-tools` folder into `AP-HomePoint-main`.
   4. **Open a command window.**
      - Mac: open **Terminal** (Applications → Utilities).
      - Windows: install [Git for Windows](https://git-scm.com/download/win) (the default choices are fine), then
        open **Git Bash** from the Start menu.
   5. **Go into the folder.** Type `cd ` (with a space after it), drag the `AP-HomePoint-main` folder into the
      window, and press Enter.
   6. **Run the installer.** Type this line, with your TV's address in place of `192.168.1.23`, and press Enter:
      - Mac: `ADB=platform-tools/adb bash scripts/setup.sh 192.168.1.23 homepoint-release.apk`
      - Windows: `ADB=platform-tools/adb.exe bash scripts/setup.sh 192.168.1.23 homepoint-release.apk`
   7. **Allow it on the TV.** The TV asks "Allow USB debugging?": tick **Always allow from this computer** and choose
      **OK**. The installer then finishes by itself and ends with "On the TV: in Home on Fire choose HomePoint...".
      If it says it couldn't reach the TV, check the address and that ADB debugging is on, then run the line again.
      If the Mac says adb can't be opened, go to System Settings → Privacy & Security, click **Allow Anyway**, and
      run the line again.

   It installs HomePoint and its companion app Home on Fire (downloaded from its official release and checked
   against a fixed checksum), and lets HomePoint read photos. To also copy your own background photos, see
   "Private files" below and add that folder's path at the end of the line.
3. On the TV: in Home on Fire, pick HomePoint as the target and turn on Replace Home (and Launch on boot).
4. Turn ADB debugging off again: Settings → My Fire TV → Developer options.

Hold Home for Amazon's home screen. To undo, turn off Home on Fire's redirect or uninstall both apps.

### Private files (not in this repo)
- `wallpapers/`: pictures for the background, copied to `/sdcard/Pictures/HomePoint/`; the first by file name is shown.
- `favorites.txt`: one package name per line, used the first time HomePoint starts, on devices that let adb write
  into an app's own folder. Fire OS 8 doesn't: there, add favorites on the TV (hold OK on an app).

## License
MIT, as upstream. Home on Fire is a separate app under GPL-3.0 and is not included.
