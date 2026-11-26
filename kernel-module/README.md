# HOS3 Visual Hybrid — KernelSU diagnostics companion

**This ZIP is optional diagnostics only. It is not the Dynamic Island engine and is not required to display the visual island.**

The actual visual engine is the HyperIsland APK, installed as a Vector/libxposed module. Vector loads the hooks; KernelSU does not replace that runtime.

## Safety contract

- Does not patch boot, init_boot, vendor_boot, recovery, or firmware partitions.
- Does not replace SystemUI, Settings, Launcher, framework, or vendor APKs.
- Includes `skip_mount`, so no `/system` overlay is mounted.
- The boot service only records device/runtime facts in `/data/adb/HOS3VisualHybrid/status.log`.
- Does not alter SELinux, system-app permissions, properties, or kernel parameters.

## Target

- Redmi Note 13 4G (`sapphire`)
- HyperOS 2 Global / Android 15
- Temporary KernelSU exploit environment
- Vector 2.2 / libxposed runtime for the APK

## Install

Only install this ZIP through the KernelSU manager if you specifically want its diagnostic status log. Do not flash it from recovery. If temporary root is lost, the diagnostic service cannot run until the root manager is available again.

After boot, inspect:

`/data/adb/HOS3VisualHybrid/status.log`

Uninstall from the root manager. Because the module uses `skip_mount`, it has no system overlay to roll back.
