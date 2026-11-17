#!/usr/bin/env bash
set -uo pipefail
status=0
mkdir -p device-demo device-screenshots
# Keep diagnostic evidence on the host even if the emulator exits.
adb logcat -v threadtime > device-screenshots/device-logcat.txt 2>&1 &
log_pid=$!
(
  while true; do
    timeout 8s adb pull /data/local/tmp/bmd-screenshots/. device-screenshots/ >/dev/null 2>&1 || true
    timeout 8s adb pull /data/local/tmp/bmd-demo/app-demo.mp4 device-demo/app-demo-raw.mp4 >/dev/null 2>&1 || true
    sleep 8
  done
) &
pull_pid=$!
timeout 12m gradle --no-daemon --stacktrace :app:connectedDebugAndroidTest || status=$?
kill "$pull_pid" "$log_pid" 2>/dev/null || true
timeout 10s adb pull /data/local/tmp/bmd-demo/app-demo.mp4 device-demo/app-demo-raw.mp4 || true
timeout 10s adb pull /data/local/tmp/bmd-screenshots/. device-screenshots/ || true
timeout 10s adb pull /sdcard/Android/data/com.bmd.scanner/files/screenshots/. device-screenshots/ || true
if [ "$status" -ne 0 ]; then
  find app/build/outputs/androidTest-results -name '*.xml' -exec cat {} +
  tail -n 150 device-screenshots/device-logcat.txt
  sudo dmesg | tail -n 40 || true
fi
exit "$status"

