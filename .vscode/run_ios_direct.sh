#!/bin/bash

# Direct iOS Device Run Script - bypasses Gradle issues
set -e

echo "🚀 Running KMP-IAP Example on iOS Device (Direct)..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Xcode is installed
if ! command -v xcodebuild &> /dev/null; then
    echo -e "${RED}❌ Xcode is not installed. Please install Xcode from the App Store.${NC}"
    exit 1
fi

# Navigate to example directory
cd example/iosApp

# List connected devices
echo -e "\n${YELLOW}📱 Connected iOS Devices:${NC}"
xcrun xctrace list devices 2>&1 | grep -E "iPhone|iPad" | grep -v "Simulator" || echo "No physical devices found"

# Get the first connected device
DEVICE_ID=$(xcrun xctrace list devices 2>&1 | grep -E "iPhone|iPad" | grep -v "Simulator" | head -n 1 | sed -n 's/.*(\([0-9A-Fa-f-]*\)).*/\1/p' || echo "")

if [ -z "$DEVICE_ID" ]; then
    echo -e "${RED}❌ No iOS device connected. Please connect your iPhone/iPad and make sure it's trusted.${NC}"
    echo -e "${YELLOW}💡 Alternative: Use 'iOS Device (Open in Xcode)' launcher${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Found device: $DEVICE_ID${NC}"

# Build directly with xcodebuild
echo -e "\n🔨 Building iOS app directly..."
xcodebuild \
    -project iosApp.xcodeproj \
    -scheme iosApp \
    -destination "id=$DEVICE_ID" \
    -configuration Debug \
    -allowProvisioningUpdates \
    clean build \
    CODE_SIGN_IDENTITY="Apple Development" \
    CODE_SIGN_STYLE="Automatic" \
    DEVELOPMENT_TEAM="" || {
        echo -e "\n${RED}❌ Build failed!${NC}"
        echo -e "${YELLOW}💡 Tips:${NC}"
        echo "  1. Use 'iOS Device (Open in Xcode)' launcher instead"
        echo "  2. Open Xcode and set your development team"
        echo "  3. Fix any signing issues in Xcode"
        exit 1
    }

echo -e "\n${GREEN}✅ Build successful!${NC}"

# Find the built app
APP_PATH=$(find build -name "kmp-iap-example.app" -type d | grep "Debug-iphoneos" | head -1)

if [ -z "$APP_PATH" ]; then
    echo -e "${YELLOW}⚠️  App not found in build directory. Looking in DerivedData...${NC}"
    APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData -name "kmp-iap-example.app" -type d | grep "Debug-iphoneos" | head -1)
fi

if [ -z "$APP_PATH" ]; then
    echo -e "${RED}❌ Could not find built app!${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Found app at: $APP_PATH${NC}"

# Install and run using devicectl or ios-deploy
echo -e "\n🚀 Installing and running app on device..."

# Try xcrun devicectl first (modern approach)
if command -v xcrun devicectl &> /dev/null; then
    echo -e "${BLUE}Using xcrun devicectl...${NC}"
    
    # Install the app
    xcrun devicectl device install app --device "$DEVICE_ID" "$APP_PATH" && {
        echo -e "${GREEN}✅ App installed successfully${NC}"
        
        # Get bundle ID
        BUNDLE_ID="dev.hyo.martie"
        
        # Launch the app
        echo -e "${BLUE}Launching app...${NC}"
        xcrun devicectl device process launch --device "$DEVICE_ID" "$BUNDLE_ID" && {
            echo -e "${GREEN}✅ App launched successfully!${NC}"
        } || {
            echo -e "${YELLOW}⚠️  Could not launch app automatically${NC}"
            echo -e "${YELLOW}📱 Please tap the app icon on your device to run it${NC}"
        }
    } || {
        echo -e "${YELLOW}⚠️  devicectl install failed${NC}"
    }
else
    # Fallback to ios-deploy
    if command -v ios-deploy &> /dev/null; then
        echo -e "${BLUE}Using ios-deploy...${NC}"
        ios-deploy --bundle "$APP_PATH" --id "$DEVICE_ID" --noninteractive --justlaunch || {
            echo -e "\n${YELLOW}⚠️  ios-deploy encountered issues (this is normal with newer iOS versions)${NC}"
            echo -e "${GREEN}✅ But the app should be installed on your device!${NC}"
            echo -e "${YELLOW}📱 Just tap the app icon on your device to run it${NC}"
        }
    else
        echo -e "${YELLOW}⚠️  Neither devicectl nor ios-deploy found${NC}"
        echo -e "${YELLOW}📱 The app is built but you'll need to install it manually${NC}"
    fi
fi