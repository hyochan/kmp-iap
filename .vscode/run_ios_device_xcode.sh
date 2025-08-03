#!/bin/bash

# Script to open Xcode for device deployment

echo "🔨 Opening Xcode for iOS device deployment..."

# Change to the example iOS app directory
cd "$(dirname "$0")/../example/iosApp"

# Open the project in Xcode
open iosApp.xcodeproj

echo "📱 Xcode opened!"
echo ""
echo "To run on device:"
echo "1. Select your device from the device dropdown"
echo "2. Make sure your team is selected in Signing & Capabilities"
echo "3. Click the Run button (or press Cmd+R)"
echo ""
echo "If you encounter code signing issues:"
echo "- Go to Signing & Capabilities tab"
echo "- Enable 'Automatically manage signing'"
echo "- Select your team from the dropdown"