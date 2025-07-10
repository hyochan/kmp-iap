# KMP-IAP Example Application

This is a Kotlin Multiplatform example project demonstrating the usage of the KMP-IAP library for in-app purchases across Android, iOS, Desktop, and Web.

## Project Structure

* `/src` contains the shared Kotlin code:
  - `commonMain` - Common code shared across all platforms
  - `androidMain` - Android-specific implementations
  - `iosMain` - iOS-specific implementations
  - `jvmMain` - Desktop (JVM) specific implementations

* `/iosApp` - iOS application entry point using SwiftUI

## Running the Example

### Android
```bash
./gradlew :example:installDebug
```

### iOS
Open `/iosApp/iosApp.xcodeproj` in Xcode and run the project.

### Desktop
```bash
./gradlew :example:run
```

### Web
```bash
./gradlew :example:wasmJsBrowserDevelopmentRun
```

## Features Demonstrated

- Library version display showing platform-specific implementation
- Basic UI for future IAP functionality

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html).