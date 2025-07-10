# KMP IAP (Kotlin Multiplatform In-App Purchase)

A Kotlin Multiplatform library boilerplate for handling in-app purchases across Android and iOS platforms.

> ⚠️ **Work in Progress**: This is currently a boilerplate project. IAP functionality will be implemented in future updates.

## Status

This project provides the basic structure for a Kotlin Multiplatform IAP library with:
- ✅ Project setup with Gradle configuration
- ✅ Multi-platform source sets (Android, iOS, JVM, WASM, Linux)
- ✅ Publishing configuration for Maven Central
- ✅ CI/CD setup with GitHub Actions
- ❌ IAP implementation (TODO)

## Supported Platforms

- Android (API 24+)
- iOS (iOS 13.0+)
- JVM/Desktop (stub)
- Web/WASM (stub)
- Linux (stub)

## Project Structure

```
kmp-iap/
├── library/
│   └── src/
│       ├── commonMain/         # Shared code
│       ├── androidMain/        # Android-specific
│       ├── iosMain/           # iOS-specific
│       ├── jvmMain/           # Desktop JVM
│       ├── wasmJsMain/        # Web WASM
│       └── linuxX64Main/      # Linux native
├── example/                    # Example Compose Multiplatform app
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       ├── iosMain/
│       └── jvmMain/
├── gradle/
│   └── libs.versions.toml     # Version catalog
├── build.gradle.kts
└── settings.gradle.kts
```

## Getting Started

See [docs/SETUP.md](docs/SETUP.md) for detailed setup instructions.

### Quick Start

```bash
# Automated setup (recommended)
./setup.sh

# Manual setup
cp local.properties.template local.properties
# Edit local.properties with your values

# Build library
./gradlew :library:build

# Run example app
./gradlew :example:run  # Desktop
./gradlew :example:installDebug  # Android
```

### VS Code Integration

The project includes VS Code launch configurations for common tasks:
- Build/test library
- Run example applications
- Publish to Maven repositories
- Generate documentation
- Code formatting

Open the project in VS Code and use **Run and Debug** panel.

## Example App

The project includes a Compose Multiplatform example app demonstrating library usage:
- Android, iOS, Desktop, and Web support
- Basic UI showcasing IAP integration (to be implemented)
- Located in the `example/` directory

## Documentation

- [Setup Guide](docs/SETUP.md) - Development environment setup
- [Release Guide](docs/RELEASE.md) - Publishing to Maven Central
- [GPG Configuration](gpg-key-spec.md) - GPG key setup for signing

## Contributing

Contributions are welcome! Feel free to implement the IAP functionality or improve the project structure.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the setup guide in [docs/SETUP.md](docs/SETUP.md)
4. Implement your changes
5. Add tests if applicable
6. Run `./gradlew spotlessApply` to format code
7. Commit your changes (`git commit -m 'Add amazing feature'`)
8. Push to the branch (`git push origin feature/amazing-feature`)
9. Open a Pull Request

## License

MIT License