#!/bin/bash

# Script to build and run iOS app on physical device

echo "🔨 Building iOS app for device..."

# Change to the example iOS app directory
cd "$(dirname "$0")/../example/iosApp"

# Build the app for device
xcodebuild -project iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination generic/platform=iOS \
  -derivedDataPath build/DerivedData \
  CODE_SIGN_IDENTITY="Apple Development" \
  CODE_SIGNING_REQUIRED=YES \
  clean build

if [ $? -eq 0 ]; then
  echo "✅ Build succeeded!"
  
  # Check if ios-deploy is installed
  if ! command -v ios-deploy &> /dev/null; then
    echo "⚠️  ios-deploy not found. Installing..."
    brew install ios-deploy
  fi
  
  # Find the app bundle
  APP_PATH=$(find build/DerivedData/Build/Products -name "*.app" -type d | head -n 1)
  
  if [ -n "$APP_PATH" ]; then
    echo "📱 Found app at: $APP_PATH"
    echo "🚀 Installing and launching app on device..."
    
    # Get the first connected device ID
    DEVICE_ID=$(ios-deploy -c | grep -v "Found" | head -n 1 | cut -d' ' -f1)
    
    if [ -n "$DEVICE_ID" ]; then
      echo "📱 Using device: $DEVICE_ID"
      ios-deploy --id "$DEVICE_ID" --bundle "$APP_PATH" --debug --no-wifi
    else
      echo "❌ No iOS device found. Please connect a device and try again."
      exit 1
    fi
  else
    echo "❌ App bundle not found!"
    exit 1
  fi
else
  echo "❌ Build failed!"
  exit 1
fi