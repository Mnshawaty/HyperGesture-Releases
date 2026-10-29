# Architecture and boundaries

HyperGesture 0.4.3 is a classic Xposed module with a native Android configuration/information activity. It hooks only the main processes of com.miui.home and com.android.systemui, on Android API 35 and the exact supported version codes.

## Navigation flow

Application.attach installs version-gated hooks. MiuiHome preflights expected classes and methods before registering its main hooks; registration failure rolls those hooks back. Gesture-window eligibility is maintained while actual HOME resolution remains unchanged.

BottomTouchHook accepts eligible third-party-home touch streams after keyguard and system-disable checks. SwipeDecision uses dp coordinates: vertical intent at 18dp, final Home/Recents displacement at 60dp, stable hold of 350ms within a 12dp movement tolerance, and horizontal completion at 72dp with directional dominance. A hold timer can dispatch Recents before release.

Accepted actions use explicit default-HOME launch, OverviewCommandHelper, or ActivityManager task reordering. Home and previous-app switching request GESTURE_END haptics; Recents requests CONFIRM. Exceptions in feedback do not abort navigation. Android can suppress feedback based on settings or device state.

## Long press

Stationary input is forwarded to Xiaomi's native gesture detector. Moving into a navigation gesture cancels the detector's long-press stream. A targeted CircleToSearchHelper gate is adjusted; Google services, the system feature and the configured shortcut must already exist.

## Limits

No interactive window animation, custom glow, or Xiaomi-engine warm-up is included. Both horizontal directions request the previous eligible task. Runtime injection, framework policies and OEM behavior cannot be established by pure-Java tests. The project does not currently include an emulator or device test harness.

