# HOS3 Visual Hybrid — project plan

## Target environment

- Redmi Note 13 4G (`sapphire`)
- HyperOS 2 Global `2.0.213.0.VNGMIXM`, Android 15
- Bootloader remains locked
- Temporary KernelSU exploit root
- Vector 2.2 active (libxposed runtime)
- Zygisk Next 1.5.3

## Architecture

1. **Vector edition (visual engine):** application-level hooks loaded by Vector. This is the only layer intended to change SystemUI/Settings behavior. Every hook must be guarded by package, class, method and signature checks; unknown signatures must be skipped.
2. **Root companion:** optional KernelSU-style module in `kernel-module/`. It must never patch boot images or write to system/vendor partitions. Initial build is diagnostic-only and uses `skip_mount`.
3. **User-space assets:** wallpapers, icons, fonts and preview resources delivered separately. Prefer normal Android theme/launcher APIs over replacing privileged system packages.
4. **Recovery path:** disable the Vector module first; uninstall the companion from its root manager. No recovery flash is part of this project.

## Hard constraints

- Do not unlock bootloader.
- Do not flash boot/init_boot/vendor_boot/recovery or a ROM.
- Do not replace `SystemUI.apk`, `MiuiSystemUI.apk`, or `MIUISystemUIPlugin.apk`.
- Do not assume HyperOS 3 classes exist on HyperOS 2.
- Do not assume KernelSU's official module manager works with an exploit-based temporary root. Verify that the environment exposes the expected module directory and installer.
- A successful build is not proof of runtime compatibility.

## Current status

- [x] Separate work branch created: `hos3/visual-hybrid`
- [x] Safe KernelSU-style diagnostic scaffold
- [x] System overlay explicitly disabled (`skip_mount`)
- [ ] Inspect Vector 2.2 module loading and package scope on the actual phone
- [ ] Build a dedicated libxposed visual module with guarded HyperOS 2 hooks
- [ ] Implement user-space assets and a launcher/theme layer
- [ ] Test SystemUI restart, notification shade, lock screen, battery and rollback on-device

## Why this is not a full HyperOS 3 port

With a locked bootloader and no partition flashing, this project can only provide a visual adaptation layered over HyperOS 2. It cannot replace the OS framework, vendor stack, kernel or boot chain. The final result must be described as a visual hybrid, not a HyperOS 3 ROM.
