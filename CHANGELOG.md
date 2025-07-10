# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-alpha02] - 2025-01-28

### Added
- Complete API design matching Flutter InApp Purchase and expo-iap
- Full type system for products, purchases, and platform-specific features
- UseIap hook for easy state management with Kotlin Coroutines Flow
- Android implementation with Google Play Billing Library v8
  - Product/subscription fetching
  - Purchase flow
  - Available purchases query
  - Transaction acknowledgment/consumption
  - Deep linking to subscriptions
- Event-based architecture with Flow support
- Error handling with comprehensive error codes
- Platform-specific request types (RequestPurchaseAndroid, RequestPurchaseIOS)

### Changed
- Removed support for JVM, WASM, and Linux platforms (focusing on mobile)
- Improved type safety with sealed classes and enums
- Better separation of platform-specific and common code

### Known Issues
- iOS StoreKit implementation is still in progress
- Receipt validation not yet implemented
- Example app needs completion

## [0.0.0-alpha1] - 2025-01-10

### Added
- Initial KMP IAP library setup
- Support for Android, iOS, Desktop (JVM), Linux, and Web (WASM) platforms
- Basic library structure with platform-specific implementations
- `getVersion()` function to display library version per platform
- Example application demonstrating library usage
- Maven Central publishing setup

### Features
- Cross-platform in-app purchase library foundation
- Platform-specific version identification
- Example app with version display