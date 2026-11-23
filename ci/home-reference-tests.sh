#!/usr/bin/env bash
set -uo pipefail
status=0
timeout 7m gradle --no-daemon --stacktrace :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bmd.scanner.HomeLayoutTest,com.bmd.scanner.HomeReferenceTest || status=$?
mkdir -p home-screenshots
timeout 15s adb pull /data/local/tmp/bmd-screenshots/. home-screenshots/ || true
timeout 10s adb logcat -d -s BMD-reference:I AndroidRuntime:E || true
if [ "$status" -ne 0 ]; then
  find app/build/outputs/androidTest-results -name '*.xml' -exec cat {} +
fi
exit "$status"
