#!/bin/bash

echo "Installation de l'APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "Lancement de l'application..."
adb shell am start -n fr.didictateur.inanutshell/fr.didictateur.inanutshell.MainActivity

echo "Affichage des logs (filtré sur ViewRecette)..."
echo "CTRL+C pour arrêter les logs"
adb logcat | grep "ViewRecette"
