#!/bin/bash
export ANDROID_SDK_ROOT=/opt/android-sdk
export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:/opt/android-sdk/emulator:/opt/android-sdk/tools:/opt/android-sdk/platform-tools
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk

cd /opt/android-sdk/emulator
./emulator -avd Medium_Phone_API_36.0

