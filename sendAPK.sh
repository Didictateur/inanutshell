#!/bin/bash

adb uninstall fr.didictateur.inanutshell
adb install -r app/build/outputs/apk/debug/app-debug.apk
cp app/build/outputs/apk/debug/app-debug.apk ~/Sun
