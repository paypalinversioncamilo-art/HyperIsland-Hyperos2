# HOS3 Visual Hybrid — Redmi Note 13 4G

## Target environment

- Redmi Note 13 4G (`sapphire`)
- HyperOS 2 Global `2.0.213.0.VNGMIXM`, Android 15
- Locked bootloader; no ROM or partition flashing
- Temporary KernelSU exploit root
- Vector 2.2 (libxposed runtime) and Zygisk Next 1.5.3

## Architecture

1. **Vector / libxposed layer:** guarded hooks for Xiaomi's native SystemUI and plugin implementation. The compatibility probe is read-only; native hooks must only be enabled after runtime class and method verification.
2. **HyperOS 2 visual fallback:** `Hos2IslandListenerService` uses Android's `NotificationListenerService` and `TYPE_APPLICATION_OVERLAY`. It draws an animated pill without modifying SystemUI. It recognizes call, media, timer/alarm, progress and ordinary notification categories; exposes up to three notification actions; supports tap-through, swipe-to-dismiss, long-press dismiss, expanded mode, progress display and entrance/exit animation.
3. **KernelSU companion:** optional diagnostic-only ZIP. It logs device/runtime information and deliberately has `skip_mount`; it does not install system overlays or modify partitions. It is not the visual engine.
4. **User-space assets:** app resources and settings. No privileged APK replacement.

## Permissions required by the fallback

The user must grant both permissions explicitly:

- **Notification access:** Settings → Notifications → Device & app notifications (wording varies by HyperOS) → HyperIsland → Allow.
- **Display over other apps:** Settings → Apps → Manage apps → HyperIsland → Other permissions / Display pop-up windows or Display over other apps → Allow. On Android this can also be opened from the app's special-app-access page.

If either permission is absent, the fallback intentionally remains hidden. The service does not silently grant itself permissions.

## Safety constraints

- Do not unlock the bootloader.
- Do not flash boot/init_boot/vendor_boot/recovery or a ROM.
- Do not replace `SystemUI.apk`, `MiuiSystemUI.apk`, or `MIUISystemUIPlugin.apk`.
- Do not assume HyperOS 3 classes exist on HyperOS 2.
- KernelSU root is not the Xposed hook runtime; Vector provides the libxposed runtime.
- A successful CI build is not proof of runtime compatibility on the handset.

## Current implementation status

- [x] Isolated branch: `hos3/visual-hybrid`
- [x] Read-only compatibility probe for SystemUI and MIUI plugin class loaders
- [x] Plugin package included in libxposed scope
- [x] Safe KernelSU diagnostic ZIP scaffold (`skip_mount`, no system writes)
- [x] Notification-listener fallback renderer with animated capsule, category labels, action buttons, progress, tap-through and swipe/long-press dismissal
- [ ] Compile and validate the latest changes with GitHub Actions
- [ ] Verify notification access and overlay behavior on the actual Redmi Note 13 4G
- [ ] Use on-device probe logs to enable only the Xiaomi-native hooks that actually exist on this firmware
- [ ] Test media, calls, timers, screen recording, lock screen, battery use and rollback on-device

## What this port is — and is not

This is a **visual hybrid layer over HyperOS 2**, not a HyperOS 3 ROM. With a locked bootloader and no partition flashing, it cannot replace the OS framework, vendor stack, kernel, or boot chain. The fallback can provide a usable dynamic-island-style overlay, while exact Xiaomi-native integration depends on classes and signatures confirmed on the device.
