#!/usr/bin/env bash
set -euo pipefail

adb devices | grep -q "device$" || { echo "No adb device connected"; exit 1; }

adb shell am force-stop com.qstarem.app
sleep 1
adb logcat -c
adb shell am start -n com.qstarem.app/.MainActivity

echo "Capturing logs for 20s…"
sleep 20

adb logcat -d | grep -E \
  "BrowserViewModel|ExtensionManager|GeckoReady|Auto-approving|Extensions synced|Installing|GeckoThread|hiddenapi|signal 9|Process com.qstarem.app|FATAL|GeckoConsole.*Error" \
  | tail -80

echo
echo "Running processes:"
adb shell "ps -A | grep qstarem" || true
