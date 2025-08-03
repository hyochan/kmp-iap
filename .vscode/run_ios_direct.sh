#!/bin/bash

# Direct iOS device deployment script with enhanced logging

set -e

echo "🚀 Direct iOS Device Deployment"
echo "=============================="

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Check for ios-deploy
if ! command -v ios-deploy &> /dev/null; then
    echo "❌ ios-deploy not found!"
    echo "Installing ios-deploy..."
    brew install ios-deploy
fi

# Check for connected devices
echo "🔍 Checking for connected iOS devices..."
DEVICE_LIST=$(ios-deploy -c 2>/dev/null || true)

if [ -z "$DEVICE_LIST" ] || [[ "$DEVICE_LIST" == *"No devices found"* ]]; then
    echo "❌ No iOS devices found!"
    echo ""
    echo "Please:"
    echo "1. Connect your iOS device via USB"
    echo "2. Trust this computer on your device"
    echo "3. Make sure your device is unlocked"
    exit 1
fi

echo "$DEVICE_LIST"

# Get first device ID
DEVICE_ID=$(echo "$DEVICE_LIST" | grep -v "Found" | head -n 1 | awk '{print $1}')

if [ -z "$DEVICE_ID" ]; then
    echo "❌ Could not extract device ID"
    exit 1
fi

echo "📱 Using device: $DEVICE_ID"

# Build the app
echo ""
echo "🔨 Building iOS app for device..."
cd "$PROJECT_ROOT/example/iosApp"

# Clean build directory
rm -rf build/DerivedData

xcodebuild -project iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -destination "id=$DEVICE_ID" \
    -derivedDataPath build/DerivedData \
    CODE_SIGN_IDENTITY="Apple Development" \
    CODE_SIGNING_REQUIRED=YES \
    CODE_SIGNING_ALLOWED=YES \
    DEVELOPMENT_TEAM="" \
    build

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    echo ""
    echo "If you see code signing errors:"
    echo "1. Open example/iosApp/iosApp.xcodeproj in Xcode"
    echo "2. Select the project in navigator"
    echo "3. Go to Signing & Capabilities"
    echo "4. Enable 'Automatically manage signing'"
    echo "5. Select your team"
    exit 1
fi

echo "✅ Build succeeded!"

# Find the app bundle
APP_PATH=$(find build/DerivedData/Build/Products -name "*.app" -type d | head -n 1)

if [ -z "$APP_PATH" ]; then
    echo "❌ Could not find built app!"
    echo "Looking in: build/DerivedData/Build/Products"
    find build/DerivedData -name "*.app" -type d
    exit 1
fi

echo "📦 Found app at: $APP_PATH"

# Deploy to device
echo ""
echo "📲 Installing app on device..."
ios-deploy --id "$DEVICE_ID" \
    --bundle "$APP_PATH" \
    --debug \
    --no-wifi \
    --justlaunch

echo ""
echo "✨ Done! The app should now be running on your device."