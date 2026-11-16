#!/usr/bin/env bash
set -uo pipefail
status=0
gradle --no-daemon --stacktrace :app:connectedDebugAndroidTest || status=$?
adb pull /data/local/tmp/bmd-screenshots device-screenshots || true
if [ "$status" -ne 0 ]; then
  find app/build/outputs/androidTest-results -name '*.xml' -exec cat {} +
fi
exit "$status"
