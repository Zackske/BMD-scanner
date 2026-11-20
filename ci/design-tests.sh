#!/usr/bin/env bash
set -uo pipefail
mkdir -p device-screenshots
adb shell wm size 720x1600
adb shell wm density 320
adb logcat -c
adb logcat -v threadtime > device-screenshots/logcat.txt 2>&1 &
log_pid=$!
# Full-sensor camera follows acceleration, not a UIAutomator user-rotation lock.
# Test requests are fixed filenames written by the instrumentation shell.
adb shell rm -f /data/local/tmp/bmd-rotate-landscape /data/local/tmp/bmd-rotate-portrait
adb emu sensor set acceleration 0:9.8:0
(
  while true; do
    if adb shell test -f /data/local/tmp/bmd-rotate-landscape; then
      adb emu sensor set acceleration 9.8:0:0
      adb shell rm -f /data/local/tmp/bmd-rotate-landscape
    fi
    if adb shell test -f /data/local/tmp/bmd-rotate-portrait; then
      adb emu sensor set acceleration 0:9.8:0
      adb shell rm -f /data/local/tmp/bmd-rotate-portrait
    fi
    sleep 1
  done
) > device-screenshots/rotation-sensor.txt 2>&1 &
sensor_pid=$!
status=0
timeout 15m gradle --no-daemon --stacktrace :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bmd.scanner.StackThemeScreensTest || status=$?
kill "$sensor_pid" 2>/dev/null || true
kill "$log_pid" 2>/dev/null || true
timeout 20s adb pull /data/local/tmp/bmd-screenshots/. device-screenshots/ || true
grep -E "BMD-real|BMD-bulk|BMD-box" device-screenshots/logcat.txt | tail -n 80 || true
if [ "$status" -ne 0 ]; then
  find app/build/outputs/androidTest-results -name '*.xml' -exec cat {} +
  grep -E 'BMD|FATAL|AssertionError' device-screenshots/logcat.txt | tail -n 100 || true
fi
exit "$status"

