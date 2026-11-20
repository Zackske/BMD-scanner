#!/usr/bin/env bash
set -uo pipefail
suite="${BMD_SUITE:-baseline}"
mkdir -p "device-screenshots/$suite"
adb shell wm size 720x1600
adb shell wm density 320
adb logcat -c
adb logcat -v threadtime > "device-screenshots/$suite/logcat.txt" 2>&1 &
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
classes="${BMD_ANDROID_CLASSES:-com.bmd.scanner.DesignValidationTest,com.bmd.scanner.CameraControlsTest,com.bmd.scanner.NativeBulkTest,com.bmd.scanner.GuidedBoxTest,com.bmd.scanner.ThemeIntegrationTest,com.bmd.scanner.ScanWizardTest,com.bmd.scanner.MetalRenderingTest}"
timeout 22m gradle --no-daemon --stacktrace -Dorg.gradle.jvmargs="-Xmx1024m -Dfile.encoding=UTF-8" :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="$classes" || status=$?
kill "$sensor_pid" 2>/dev/null || true
kill "$log_pid" 2>/dev/null || true
timeout 20s adb pull /data/local/tmp/bmd-screenshots/. device-screenshots/ || true
grep -E "BMD-real|BMD-bulk|BMD-box" "device-screenshots/$suite/logcat.txt" | tail -n 80 || true
if [ "$status" -ne 0 ]; then
  find app/build/outputs/androidTest-results -name '*.xml' -exec cat {} +
  sudo dmesg | tail -n 40 || true
  free -m
  ps -eo pid,rss,comm --sort=-rss | head -n 12
  tail -n 120 "device-screenshots/$suite/logcat.txt" || true
  grep -E 'BMD|FATAL|AssertionError' "device-screenshots/$suite/logcat.txt" | tail -n 100 || true
fi
cp -R app/build/outputs/androidTest-results "device-screenshots/$suite/results" 2>/dev/null || true
exit "$status"

