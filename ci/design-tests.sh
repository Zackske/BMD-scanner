#!/usr/bin/env bash
set -uo pipefail
mkdir -p device-screenshots
adb shell wm size 720x1600
adb shell wm density 320
adb logcat -c
adb logcat -v threadtime > device-screenshots/logcat.txt 2>&1 &
log_pid=$!
status=0
timeout 9m gradle --no-daemon --stacktrace :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bmd.scanner.DesignValidationTest,com.bmd.scanner.CameraControlsTest || status=$?
kill "$log_pid" 2>/dev/null || true
timeout 20s adb pull /data/local/tmp/bmd-screenshots/. device-screenshots/ || true
if [ "$status" -ne 0 ]; then
  find app/build/outputs/androidTest-results -name '*.xml' -exec cat {} +
  grep -E 'BMD|FATAL|AssertionError' device-screenshots/logcat.txt | tail -n 100 || true
fi
exit "$status"

