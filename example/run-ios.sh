#!/bin/bash

# Run iOS app script

echo "Building iOS app with CocoaPods (openiap 1.1.6)..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT/example"

# Ensure CocoaPods are installed for the KMP library and link the Pod-backed framework
../gradlew :library:podInstall
../gradlew :library:linkPodDebugFrameworkIosSimulatorArm64

# Build and run the iOS app
cd iosApp
xcodebuild \
    -project iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -sdk iphonesimulator \
    -derivedDataPath build/DerivedData \
    -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=latest' \
    build

# Launch in simulator
DEVICE_NAME="iPhone 16 Pro"
xcrun simctl boot "$DEVICE_NAME" 2>/dev/null || true
xcrun simctl install "$DEVICE_NAME" build/DerivedData/Build/Products/Debug-iphonesimulator/kmp-iap-example.app
xcrun simctl launch "$DEVICE_NAME" dev.hyo.martie
