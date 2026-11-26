#!/system/bin/sh
# HOS3 Visual Hybrid: safe diagnostic companion.
# This first build intentionally does not replace or mount SystemUI resources.
MODDIR=${0%/*}
STATE_DIR=/data/adb/HOS3VisualHybrid
LOG_FILE="$STATE_DIR/status.log"
mkdir -p "$STATE_DIR"
chmod 0700 "$STATE_DIR"
{
  echo "----- HOS3 Visual Hybrid boot check -----"
  date 2>/dev/null
  echo "device=$(getprop ro.product.device)"
  echo "model=$(getprop ro.product.model)"
  echo "android=$(getprop ro.build.version.release)"
  echo "sdk=$(getprop ro.build.version.sdk)"
  echo "abi=$(getprop ro.product.cpu.abi)"
  if [ "$(getprop ro.product.device)" = "sapphire" ]; then
    echo "target=SUPPORTED_DEVICE"
  else
    echo "target=UNVERIFIED_DEVICE"
  fi
  if [ -d /data/adb/modules ]; then
    echo "module_manager=KERNELSU_STYLE_MODULE_DIR_PRESENT"
  else
    echo "module_manager=NOT_AVAILABLE"
  fi
  echo "mode=SAFE_NO_SYSTEM_MOUNT"
  echo "vector_hooks=provided_by_separate_libxposed_apk"
  echo "----- end -----"
} >> "$LOG_FILE" 2>&1
chmod 0600 "$LOG_FILE"
exit 0
