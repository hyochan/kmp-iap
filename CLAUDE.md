# CLAUDE.md - Project Conventions and Guidelines

This document outlines the coding conventions and guidelines for the kmp-iap project. Contributors and users can reference this for maintaining consistency.

## Naming Conventions

### IAP Acronym Usage
When using "IAP" (In-App Purchase) in class/variable names:

1. **When IAP is the final suffix**: Use all caps `IAP`
   - ✅ `KmpIAP` (class name ending with IAP)
   - ✅ `MyServiceIAP`

2. **When IAP is followed by other words**: Use camelCase `Iap`
   - ✅ `KmpIapInstance` (IAP followed by "Instance")
   - ✅ `IapManager` (IAP followed by "Manager")
   - ✅ `kmpIapInstance` (variable name)

### Examples
```kotlin
// Class names
class KmpIAP()          // ✅ Correct - IAP is final
class KmpIapInstance    // ✅ Correct - IAP followed by Instance
class KmpIAPInstance    // ❌ Wrong - should be KmpIapInstance

// Variable names
val kmpIAP = KmpIAP()        // ✅ Correct - instance of KmpIAP
val kmpIapInstance: KmpIAP   // ✅ Correct - follows camelCase
val kmpIAPInstance: KmpIAP   // ❌ Wrong - should be kmpIapInstance
```

## API Design Patterns

### Instance Creation
The library supports two patterns for maximum flexibility:

1. **Global Instance** (for convenience)
   ```kotlin
   import io.github.hyochan.kmpiap.kmpIapInstance
   
   kmpIapInstance.initConnection()
   ```

2. **Constructor Pattern** (for testing and DI)
   ```kotlin
   import io.github.hyochan.kmpiap.KmpIAP
   
   val kmpIAP = KmpIAP()
   kmpIAP.initConnection()
   ```

## Code Style Guidelines

### General Principles
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use instance-based patterns over static/singleton patterns in examples
- Provide clear comments for receipt validation and server-side processing

### Purchase Flow Pattern
```kotlin
// 1. Listen for purchase updates
kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
    // 2. Validate receipt with your backend
    val isValid = validateReceiptOnServer(purchase)
    
    if (isValid) {
        // 3. Grant entitlement
        grantEntitlement(purchase.productId)
        
        // 4. Finish transaction
        kmpIapInstance.finishTransaction(
            purchase = purchase,
            isConsumable = true // Set based on product type
        )
    }
}
```

## Testing Commands

### Build Library
```bash
./gradlew :library:build
```

### Run Example App
```bash
# Android
./gradlew :example:composeApp:assembleDebug

# iOS (requires Mac)
cd example/iosApp
xed .
```

## Contributing

When contributing to this project:
1. Follow the naming conventions outlined above
2. Ensure all tests pass
3. Update documentation if API changes are made
4. Add comments for complex logic, especially around platform-specific code