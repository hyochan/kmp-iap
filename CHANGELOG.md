# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-beta.2] - 2025-08-04

### Added

- Comprehensive documentation updates and version alignment
- Improved installation guides and examples

### Changed

- Updated all documentation references to use consistent version numbering
- Enhanced API documentation with current version references

## [1.0.0-beta.1] - 2025-02-03

### Added

- Full iOS implementation with StoreKit 2 and StoreKit 1 fallback
- Complete feature parity between Android and iOS platforms
- Comprehensive documentation site at [kmp-iap.hyo.dev](https://kmp-iap.hyo.dev)
- API reference documentation for all public APIs
- Step-by-step guides for common use cases
- Example implementations (basic store, subscription store, complete implementation)
- VS Code launch configurations for documentation site

### Changed

- Improved error handling consistency across platforms
- Enhanced type safety for platform-specific features
- Better StateFlow management in UseIap helper

### Fixed

- Transaction finishing on iOS
- Subscription offer handling on Android
- Memory leaks in Flow collectors

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
