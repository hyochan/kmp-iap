---
title: AI Assistants
sidebar_position: 7
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# AI Assistants

<IapKitBanner />

kmp-iap provides AI-optimized documentation designed to work seamlessly with modern coding assistants like Cursor, GitHub Copilot, Claude, and ChatGPT.

## AI-Optimized Documentation

| File | Description | Lines | Best For |
|------|-------------|-------|----------|
| [llms.txt](/llms.txt) | Quick reference | ~300 | Fast lookups, basic implementation |
| [llms-full.txt](/llms-full.txt) | Complete API reference | ~1000 | Complex implementations, troubleshooting |

## Adding to Cursor

1. Open Cursor Settings (`Cmd/Ctrl + ,`)
2. Navigate to **Features** → **Docs**
3. Click **Add new doc**
4. Enter the URL: `https://hyochan.github.io/kmp-iap/llms.txt`
5. Name it "kmp-iap"
6. Click **Confirm**

Now you can reference kmp-iap documentation in Cursor by typing `@kmp-iap` in your prompts.

## Using with GitHub Copilot

Reference the documentation directly in Copilot Chat:

```
@workspace Using the documentation at https://hyochan.github.io/kmp-iap/llms.txt,
help me implement in-app purchases with kmp-iap
```

Or for more detailed help:

```
@workspace Based on https://hyochan.github.io/kmp-iap/llms-full.txt,
show me how to handle subscription upgrades
```

## Using with Claude / ChatGPT

### Option 1: Include URL in Prompt

```
Using the kmp-iap documentation at https://hyochan.github.io/kmp-iap/llms-full.txt,
help me implement a purchase flow with server-side validation using IAPKit.
```

### Option 2: Paste Documentation Content

1. Open [llms.txt](/llms.txt) or [llms-full.txt](/llms-full.txt)
2. Copy the content
3. Paste into your conversation with context about your task

## Direct URL Access

Access the documentation files directly:

- **Quick Reference:** [https://hyochan.github.io/kmp-iap/llms.txt](https://hyochan.github.io/kmp-iap/llms.txt)
- **Full Reference:** [https://hyochan.github.io/kmp-iap/llms-full.txt](https://hyochan.github.io/kmp-iap/llms-full.txt)

## What's Included

### Quick Reference (llms.txt)

- Project overview and installation
- Quick start guide with both usage patterns
- Core API reference (connection, products, purchases)
- Key type definitions
- Event listeners setup
- Common patterns and examples
- Error handling basics
- Platform requirements

### Full Reference (llms-full.txt)

Everything in the quick reference, plus:

- Complete installation options (Gradle, Version Catalog)
- Detailed connection management
- All product loading methods
- Complete purchase operations with all options
- Transaction management (iOS & Android specific)
- Subscription management with all properties
- Purchase verification (native & IAPKit)
- All event listeners with examples
- Complete iOS-specific APIs
- Complete Android-specific APIs
- Alternative billing setup (Android)
- Billing programs API (Google Play 8.2.0+)
- Complete type definitions (all fields)
- Full error codes reference with descriptions
- Complete implementation patterns
- Troubleshooting guide
- Platform setup requirements

## Example Prompts

Here are some example prompts to get the most out of kmp-iap with AI assistants:

### Basic Implementation

```
Using kmp-iap documentation, create a basic IAPManager class that:
1. Initializes the store connection
2. Loads products
3. Handles purchases
4. Finishes transactions after validation
```

### Subscription Handling

```
Using kmp-iap's llms-full.txt documentation, show me how to:
1. Check if a user has an active subscription
2. Handle subscription upgrades on Android
3. Check expiration dates on iOS
```

### Error Handling

```
Based on kmp-iap error codes, implement comprehensive error handling
for purchases including retry logic for network errors.
```

### Purchase Verification

```
Using kmp-iap with IAPKit verification, implement a secure purchase
flow that validates purchases before granting entitlements.
```

### Alternative Billing (Android)

```
Implement Android alternative billing flow using kmp-iap, including:
1. Checking availability
2. Showing required dialog
3. Creating reporting token
4. Error handling
```

### Platform-Specific Features

```
Show me all iOS-specific APIs available in kmp-iap, including
refund requests, promotional offers, and external purchase links.
```

## Tips for Better Results

1. **Be specific about the platform** - Mention if you need iOS-specific, Android-specific, or cross-platform code

2. **Reference the correct doc** - Use `llms.txt` for quick answers, `llms-full.txt` for detailed implementations

3. **Include context** - Mention your Kotlin version, target platforms, and any specific requirements

4. **Ask about OpenIAP** - kmp-iap follows the OpenIAP specification, so asking about OpenIAP patterns works well

5. **Request complete examples** - Ask for full code examples including error handling and edge cases

## Feedback

If you have suggestions for improving the AI documentation:

- Open an issue on [GitHub](https://github.com/hyochan/kmp-iap/issues)
- Join the discussion at [OpenIAP Discussions](https://github.com/hyochan/openiap.dev/discussions)

## See Also

- [Core Methods](../api/core-methods.md) - Complete API documentation
- [Types](../api/types.md) - All type definitions
- [Error Codes](../api/error-codes.md) - Error handling reference
- [OpenIAP Specification](https://openiap.dev) - Cross-platform IAP standard
