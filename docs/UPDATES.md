# GitHub update publishing

The app reads the latest stable release from `Mnshawaty/HyperGesture-Releases`.
This public repository contains the source and official releases.
No GitHub credential is included in the app. Drafts and prereleases are not offered.

## Publish every update

1. Increase `android:versionCode` in AndroidManifest.xml (current: 19) and set `versionName`.
2. Update UI version labels in MainActivity.java. Keep the package name unchanged.
3. Build using the original private release key via HG_KEYSTORE, HG_KEY_ALIAS and HG_STORE_PASSWORD. Never commit the key or credentials.
4. Prepare release assets, using the final signed APK:

```sh
python3 scripts/prepare_release.py HyperGesture-0.4.9.apk --build-tools /path/to/android-build-tools
```

The script verifies the release certificate and extracts package/version/minSdk directly
from the APK. It writes the signed APK and `update.json` into `build/release`.
Do not change the APK after generating metadata. The SHA-256 must match exactly.

5. Create a draft GitHub release in **Mnshawaty/HyperGesture-Releases**, with a unique tag
   such as `v0.4.9`. Copy `release/RELEASE_TEMPLATE.md` into its notes and fill the changes.
6. Attach BOTH generated files. Keep the metadata filename exactly `update.json` and the
   APK filename exactly as generated. Publish as a stable release and mark it **Latest**.
7. Older installed builds with the updater detect it on the next successful check.
   Version 0.4.3 has no updater and needs the initial 0.4.4 installation manually.

Do not replace an existing release with different APK bytes. Publish a new tag and
higher versionCode for a correction, even if the marketing version is unchanged.

## User experience

- A green update icon appears beside the header version only when an update is available. Tap it for download/install; tap the version for a manual check. There is no update section in Home or About. About starts with Buy me a coffee.
- App launch checks GitHub and updates the header indicator automatically.
- A persisted JobScheduler job checks roughly every 12 hours when online. Android/HyperOS
  battery policies can delay it; force-stopping the app prevents jobs until reopened.
- Manual Check for updates always makes a fresh request. Network errors and GitHub rate
  limits are shown without declaring the app current. A known cached release remains usable.
- First launch requests the Android notification permission once if needed. There is no notification settings button. Alerts are
  sent once per newer build when notifications/channel are enabled.
- Download is user initiated. Keep the app open; closing the activity cancels the transfer. Switching tabs preserves progress.
  Rotations are handled in place. Reopening allows retry; partial transfers are removed.
- The download is bounded to 50 MiB, checks size and SHA-256, package, version, minSdk and
  current signer identity. HTTPS redirects are restricted to GitHub download hosts.
- Only the single verified APK is exposed via a read-only private content provider grant.
  Android's installer performs the authoritative APK signature check and asks the user
  to approve installation. Unknown-app installation permission is requested when needed.
- Reboot after installing to replace the LSPosed hooks already loaded in system processes.

## Validation boundaries

Build/signature checks and pure-Java policy/classifier tests run on the build host.
Android installer, background scheduling, permission dialogs and notifications require
physical-device validation; do not represent these as verified by host tests.

## Update schema (attached as update.json)

`schemaVersion`, `packageName`, `versionCode`, `versionName`, `minSdk`, `apkAsset`,
`size` (bytes), `sha256` (64 hex characters). The APK must be an asset in the same release.
Release notes come from the GitHub release body. Compare numeric versionCode, not tags.

## Device smoke test

1. Open App updates on current build: expect You're up to date; no install button.
2. Test offline: expect an error with working Retry, not a false up-to-date result.
3. On an older updater-enabled test build, publish a newer signed build in the feed.
   Check manually: verify notes, progress and notification opt-in. Install and reboot.
4. Reject unknown-app permission and then grant it: verify cancellation/retry and return.
5. Test wrong checksum/signer/package only in an isolated test feed; all must be rejected.
6. Confirm Home, Recents, Back, switching and indicator long press still work.

