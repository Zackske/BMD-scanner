#!/usr/bin/env bash
set -uo pipefail
status=0
gradle --no-daemon --stacktrace :app:connectedDebugAndroidTest || status=$?
adb shell ls -l /data/local/tmp/bmd-screenshots || true
adb pull /data/local/tmp/bmd-screenshots device-screenshots || true
adb shell cat /data/local/tmp/bmd-picker.xml || true
if [ "$status" -ne 0 ]; then
  find app/build/outputs/androidTest-results -name '*.xml' -exec cat {} +
  adb logcat -d -s System.out:I AndroidRuntime:E
fi
exit "$status"
