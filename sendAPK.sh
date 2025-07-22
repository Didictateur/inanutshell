#!/bin/bash

./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n fr.didictateur.inanutshell/fr.didictateur.inanutshell.MainActivity && adb logcat | grep -i "inanutshell\|crash\|exception\|error"