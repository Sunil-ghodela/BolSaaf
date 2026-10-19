#!/usr/bin/env bash
# Run isolated Compose UI tests without Gradle/UTP's uninstall-after-test cleanup.
# Never uninstall the app or clear data: an incompatible signing key must fail safely.
set -euo pipefail
cd "$(dirname "$0")/.."
ADB="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/Users/vaibhav/Library/Android/sdk}}/platform-tools/adb"
SERIAL="${ANDROID_SERIAL:-emulator-5554}"
if [[ "$SERIAL" != emulator-* ]]; then
  echo "Live UI smoke tests require an emulator, not a personal phone." >&2
  exit 1
fi
"$ADB" -s "$SERIAL" get-state
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest
"$ADB" -s "$SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s "$SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$ADB" -s "$SERIAL" shell am instrument -w \
  -e class com.reelvoice.LiveScreenTest \
  com.reelvoice.test/androidx.test.runner.AndroidJUnitRunner \
  | tee build/live-recording-instrumentation.log
grep -Eq '^OK \([0-9]+ tests?\)' build/live-recording-instrumentation.log
