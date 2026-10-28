# HyperGesture

System navigation gestures with third-party launchers on supported **HyperOS 3 / Android 15** builds.

[Download the latest signed APK](https://github.com/Mnshawaty/HyperGesture-Releases/releases/latest) · [Buy me a coffee](https://paypal.me/mhdnashawati/5)

## Features
- Home, Recents, native Back, previous-app switching and full-screen indicator long press.
- Circle to Search where supported by the system.
- Automatic haptic feedback.
- In-app GitHub updates starting with 0.4.4: check, read notes, download and install with Android confirmation.

Smart Launcher 6.6 build 024 is the tested example, not a requirement. Other third-party launchers are not restricted, but have not all been tested.

## Requirements and setup
Android 15; MiuiHome versionCode 601052343; SystemUI versionCode 202408080; root and LSPosed/Vector. Keep Xiaomi's launcher installed.

Install the APK, select System launcher and System UI in the module scope, and reboot. Choose your preferred third-party launcher and enable gesture navigation.

## Updates
Open **App updates** on Home or About. Enable notifications there for background alerts. The app checks periodically (roughly every 12 hours, subject to Android battery scheduling). Downloads are manual; keep the update screen open. It verifies SHA-256, package, increasing build number and signer identity. Android confirms the installation. Reboot afterwards.

Versions before 0.4.4 do not contain the updater: install 0.4.4 manually once.

Each stable release contains a signed APK and `update.json`. This public repository distributes releases; source development is maintained separately. No account token or signing key is distributed here.

## Contact
- [Reddit: u/mnshawaty](https://www.reddit.com/user/mnshawaty/)
- [Telegram: @mnshawaty](https://t.me/mnshawaty)
- [Let's APK](https://t.me/letsapk)

© 2026 Mohammad Alnashawati. Third-party notices are included inside the APK.
