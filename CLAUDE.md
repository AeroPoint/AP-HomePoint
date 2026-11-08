# HomePoint

A home screen (launcher) for Fire TV and Android TV, written in Kotlin / Compose for TV. Fork of
arashvakil/HomeScreen (MIT): keep LICENSE and the credit in README.md.

## What it is
- Favorites first, then "Recently used" (apps opened from HomePoint that aren't favorites), then all other apps.
- Live TV tile: ACTION_VIEW on the hardware tuner's channel list (TVs without a tuner get no tile).
- Photo background from the app's own folder `files/wallpapers/` (first picture by name), pushed with adb.
- First-run favorites from an optional `files/favorites.txt` (one package name per line), pushed with adb.
- The Home button redirect and launch at boot come from the separate app Home on Fire (GPL-3.0; not forked, not
  bundled). Holding Home opens Amazon's home, which keeps its own Continue watching rows.

## Rules
- No internet: no INTERNET permission, no networking libraries, no updater, no telemetry.
- Household data never goes in this repo: no names, favorites lists, photos, device addresses or models, or paths
  of the people who use it. Those live in a private folder outside the repo (AP-HomePoint-private next to it,
  never committed here) and reach the TV only through `scripts/setup.sh`.
- Episode-level Continue watching is not available to third-party launchers on Fire OS: other apps' watch_next /
  preview rows need ACCESS_ALL_EPG_DATA (signature|privileged). Don't promise it.
- Never commit signing material. CI signs release builds in its last step from the secrets HOMEPOINT_KEYSTORE_B64
  and HOMEPOINT_KEYSTORE_PASSWORD (never on pull requests). Install only release builds on a TV: debug builds are
  signed with a different key on each CI run and can't update each other.
- Dependencies are locked (app/gradle.lockfile). After changing them, run the workflow with write_locks and commit
  the new lock file.
- Commits use the account's GitHub no-reply email (this repo's local git config).

## Checks
CI runs `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug`.
On a TV: D-pad focus, open each favorite (Live TV too), Recently used updates and skips favorites, arrange and
reload, Home from another app returns here, holding Home reaches Amazon's home, launch after a reboot.
