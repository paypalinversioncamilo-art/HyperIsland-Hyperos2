# HOS3 Visual Hybrid — Kernel module companion

This is the safe first-stage module scaffold for Redmi Note 13 4G (sapphire).

## Safety contract

- Does not patch boot, init_boot, vendor_boot, recovery, or any firmware partition.
- Does not replace SystemUI, Settings, Launcher, framework, or vendor APKs.
- Includes `skip_mount`, so no `/system` overlay is mounted.
- The boot service only records basic compatibility facts in `/data/adb/HOS3VisualHybrid/status.log`.
- It does not alter SELinux, properties, permissions of system apps, or kernel parameters.

## Important limitation

This is a diagnostic companion, not yet a visual theme. A KernelSU-style ZIP cannot provide a full HyperOS 3 appearance by itself. Visual behavior must be implemented by a compatible Vector/libxposed module and safe user-space assets. This scaffold deliberately avoids pretending that a placeholder ZIP changes the UI.

## Target

- Device: Redmi Note 13 4G, codename `sapphire`
- Base: HyperOS 2 / Android 15
- Root: temporary KernelSU exploit environment (must expose the standard module installer and persist module files under `/data/adb/modules`)
- Hook runtime: Vector 2.2 / libxposed

## Install

Install the ZIP through the KernelSU manager only if its module installer is available and accepts third-party modules. Do not flash it from custom recovery. If the temporary root session is lost, the module cannot run until the root manager/runtime is available again.

After reboot, inspect:
`/data/adb/HOS3VisualHybrid/status.log`

Uninstall from the root manager. Because the module uses `skip_mount`, it has no system overlay to roll back.
