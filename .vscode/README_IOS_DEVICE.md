# Running iOS App on Physical Device

This guide explains how to run the KMP IAP example app on a physical iOS device.

## Prerequisites

1. **Xcode**: Make sure you have Xcode installed
2. **Apple Developer Account**: You need to be signed in to Xcode with an Apple ID
3. **ios-deploy**: Install via Homebrew:
   ```bash
   brew install ios-deploy
   ```

## Setup Steps

### 1. Connect Your Device

1. Connect your iOS device to your Mac via USB
2. Trust the computer on your device when prompted

### 2. Configure Code Signing

1. Open the iOS project in Xcode:
   ```bash
   open example/iosApp/iosApp.xcodeproj
   ```

2. Select the project in the navigator
3. Go to "Signing & Capabilities" tab
4. Select your team from the dropdown
5. Xcode will automatically manage provisioning profiles

### 3. Run on Device

You have several options to run on device:

#### Option A: Using VS Code Launch Configuration
1. Press `Cmd+Shift+P` and select "Debug: Select and Start Debugging"
2. Choose "iOS Device" configuration

#### Option B: Using Shell Script
```bash
./vscode/run_ios_device.sh
```

#### Option C: Using Xcode
1. Select your device from the device dropdown in Xcode
2. Click the Run button

## Troubleshooting

### "Unable to launch" error
- Make sure your device is unlocked
- Check that you've trusted the developer certificate on your device:
  - Go to Settings > General > Device Management
  - Trust your developer certificate

### Code signing errors
- Ensure you're signed in to Xcode with an Apple ID
- Check that automatic signing is enabled
- Try cleaning the build folder: `Cmd+Shift+K` in Xcode

### ios-deploy not found
- Install it using: `brew install ios-deploy`
- Make sure Homebrew's bin directory is in your PATH