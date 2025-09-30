---
title: Basic Store Implementation
sidebar_label: Basic Store
sidebar_position: 1
---

# Basic Store Implementation

A simple store implementation demonstrating core kmp-iap concepts and basic purchase flow. Perfect for getting started with in-app purchases in Kotlin Multiplatform.

## Key Features Demonstrated

- ✅ **Connection Management** - Initialize and manage store connection
- ✅ **Product Loading** - Fetch products from both App Store and Google Play
- ✅ **Purchase Flow** - Complete purchase process with user feedback
- ✅ **Transaction Finishing** - Properly complete transactions
- ✅ **Error Handling** - Handle common purchase errors gracefully
- ✅ **Platform Differences** - Handle iOS and Android specific requirements

## Platform Differences

⚠️ **Important**: This example handles key differences between iOS and Android:

- **iOS**: Uses StoreKit 2 (iOS 15+) implemented in Swift
- **Android**: Uses Google Play Billing Library v7
- **Receipt Handling**: Different receipt formats and validation approaches
- **Transaction States**: Platform-specific state management

## Complete Implementation

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BasicStoreApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BasicStoreScreen()
            }
        }
    }
}

class BasicStoreViewModel : ViewModel() {
    
    // State management
    data class StoreState(
        val isConnected: Boolean = false,
        val isLoading: Boolean = false,
        val products: List<Product> = emptyList(),
        val errorMessage: String? = null,
        val latestPurchase: Purchase? = null,
        val processingProductId: String? = null
    )
    
    private val _state = MutableStateFlow(StoreState())
    val state: StateFlow<StoreState> = _state.asStateFlow()
    
    // Product IDs - Replace with your actual product IDs
    private val productIds = listOf(
        "coins_100",
        "coins_500", 
        "remove_ads",
        "premium_upgrade"
    )
    
    init {
        initializeStore()
        observeStates()
    }
    
    private fun initializeStore() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Initialize connection using kmpIapInstance
                kmpIapInstance.initConnection()
                _state.update { it.copy(isConnected = true) }

                // Load products after connection
                loadProducts()

            } catch (e: Exception) {
                showError("Failed to initialize store: ${e.message}")
                _state.update { it.copy(isConnected = false) }
            }
        }
    }
    
    private fun observeStates() {
        // Observe purchase updates
        viewModelScope.launch {
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                handlePurchaseSuccess(purchase)
            }
        }
        
        // Observe purchase errors
        viewModelScope.launch {
            kmpIapInstance.purchaseErrorListener.collect { error ->
                handlePurchaseError(error)
            }
        }
    }
    
    private suspend fun loadProducts() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        
        try {
            val products = kmpIapInstance.fetchProducts {
                skus = productIds
                type = ProductQueryType.InApp
            }
            _state.update { it.copy(products = products, isLoading = false) }

            products.forEach { product ->
                println("Product: ${product.id} - ${product.displayPrice}")
            }

        } catch (e: PurchaseError) {
            showError("Failed to load products: ${e.message}")
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }
    
    private suspend fun handlePurchaseSuccess(purchase: Purchase) {
        println("✅ Purchase successful: ${purchase.productId}")
        
        _state.update { 
            it.copy(
                latestPurchase = purchase,
                errorMessage = null,
                processingProductId = null
            )
        }
        
        try {
            // 1. Here you would typically verify the purchase with your server
            val isValid = verifyPurchase(purchase)
            
            if (isValid) {
                // 2. Deliver the product to the user
                deliverProduct(purchase.productId)
                
                // 3. Finish the transaction
                kmpIapInstance.finishTransaction(
                    purchase = purchase.toPurchaseInput(),
                    isConsumable = isConsumableProduct(purchase.productId)
                )

                println("✅ Purchase completed and delivered")

                // 4. Clear purchase state
                _state.update { it.copy(latestPurchase = null) }
            } else {
                showError("Purchase verification failed")
            }
            
        } catch (e: Exception) {
            showError("Error processing purchase: ${e.message}")
        }
    }
    
    private fun handlePurchaseError(error: PurchaseError) {
        println("❌ Purchase failed: ${error.message}")
        
        _state.update { 
            it.copy(
                latestPurchase = null,
                processingProductId = null
            )
        }
        
        // Handle specific error codes
        when (error.code) {
            ErrorCode.UserCancelled -> {
                // Don't show error for user cancellation
                println("User cancelled purchase")
            }

            ErrorCode.NetworkError -> {
                showError("Network error. Please check your connection and try again.")
            }

            ErrorCode.AlreadyOwned -> {
                showError("You already own this item. Try restoring your purchases.")
            }

            else -> {
                showError(error.message)
            }
        }
    }
    
    // Verify purchase with server (mock implementation)
    private suspend fun verifyPurchase(purchase: Purchase): Boolean {
        // In a real app, send the receipt to your server for verification
        println("🔍 Verifying purchase: ${purchase.productId}")
        println("Receipt: ${purchase.transactionReceipt?.take(50)}...")
        
        // Simulate server verification
        return try {
            // In production, make actual API call to your server
            // val result = api.verifyPurchase(purchase)
            // return result.isValid
            true // Mock successful verification
        } catch (e: Exception) {
            println("Verification failed: ${e.message}")
            false
        }
    }
    
    // Deliver the purchased product to the user
    private fun deliverProduct(productId: String) {
        println("🎁 Delivering product: $productId")
        
        // Implement your product delivery logic here
        when (productId) {
            "coins_100" -> {
                // Add 100 coins to user's account
                println("Added 100 coins to user account")
            }
            
            "coins_500" -> {
                // Add 500 coins to user's account
                println("Added 500 coins to user account")
            }
            
            "remove_ads" -> {
                // Remove ads for user
                println("Removed ads for user")
            }
            
            "premium_upgrade" -> {
                // Upgrade user to premium
                println("Upgraded user to premium")
            }
            
            else -> {
                println("Unknown product: $productId")
            }
        }
    }
    
    // Check if a product is consumable
    private fun isConsumableProduct(productId: String): Boolean {
        // Define which products are consumable
        val consumableProducts = listOf("coins_100", "coins_500")
        return consumableProducts.contains(productId)
    }
    
    // Make a purchase
    fun makePurchase(productId: String) {
        if (!_state.value.isConnected) {
            showError("Not connected to store")
            return
        }
        
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    processingProductId = productId,
                    errorMessage = null
                )
            }
            
            try {
                kmpIapInstance.requestPurchase {
                    ios {
                        sku = productId
                        quantity = 1
                    }
                    android {
                        skus = listOf(productId)
                    }
                }

                println("🛒 Purchase requested for: $productId")

            } catch (e: Exception) {
                showError("Failed to request purchase: ${e.message}")
                _state.update { it.copy(processingProductId = null) }
            }
        }
    }
    
    // Restore purchases
    fun restorePurchases() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Get available purchases
                val purchases = kmpIapInstance.getAvailablePurchases()
                purchases.forEach { purchase ->
                    // Process non-consumable purchases
                    if (!isConsumableProduct(purchase.productId)) {
                        deliverProduct(purchase.productId)
                    }
                }
                
                showMessage("Restored ${purchases.size} purchases")
                
            } catch (e: Exception) {
                showError("Failed to restore purchases: ${e.message}")
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun reloadProducts() {
        viewModelScope.launch {
            loadProducts()
        }
    }
    
    private fun showError(message: String) {
        _state.update { it.copy(errorMessage = message) }
    }
    
    private fun showMessage(message: String) {
        // In a real app, show a snackbar or toast
        println("ℹ️ $message")
    }
    
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
    
    private fun getUserId(): String {
        // Return a hashed user ID for fraud prevention
        return "user_123_hash"
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up connections
        viewModelScope.launch {
            kmpIapInstance.endConnection()
        }
    }
}

@Composable
fun BasicStoreScreen(
    viewModel: BasicStoreViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Basic Store") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (state.isConnected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                ),
                actions = {
                    IconButton(onClick = viewModel::reloadProducts) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                    IconButton(onClick = viewModel::restorePurchases) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Connection status
            ConnectionStatus(isConnected = state.isConnected)
            
            // Error message
            state.errorMessage?.let { error ->
                ErrorBanner(
                    message = error,
                    onDismiss = viewModel::clearError
                )
            }
            
            // Latest purchase info
            state.latestPurchase?.let { purchase ->
                PurchaseInfo(purchase = purchase)
            }
            
            // Products list
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.products.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    
                    state.products.isEmpty() -> {
                        EmptyProducts(onReload = viewModel::reloadProducts)
                    }
                    
                    else -> {
                        ProductsList(
                            products = state.products,
                            processingProductId = state.processingProductId,
                            onPurchase = viewModel::makePurchase
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatus(isConnected: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isConnected) 
            MaterialTheme.colorScheme.primaryContainer
        else 
            MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (isConnected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isConnected) "Connected to Store" else "Not Connected",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isConnected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun PurchaseInfo(purchase: Purchase) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Purchase Successful!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Product: ${purchase.productId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "Transaction: ${purchase.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun EmptyProducts(onReload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Store,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No products available",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onReload) {
            Text("Reload Products")
        }
    }
}

@Composable
fun ProductsList(
    products: List<Product>,
    processingProductId: String?,
    onPurchase: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products) { product ->
            ProductCard(
                product = product,
                isProcessing = product.id == processingProductId,
                onPurchase = { onPurchase(product.id) }
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    isProcessing: Boolean,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getProductIcon(product.id),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    product.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.displayPrice,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Button(
                    onClick = onPurchase,
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Buy Now")
                    }
                }
            }
        }
    }
}

fun getProductIcon(productId: String): ImageVector {
    return when (productId) {
        "coins_100", "coins_500" -> Icons.Default.MonetizationOn
        "remove_ads" -> Icons.Default.Block
        "premium_upgrade" -> Icons.Default.Star
        else -> Icons.Default.ShoppingBag
    }
}
```

## Key Features Explained

### 1. Connection Management
```kotlin
kmpIapInstance.initConnection()
```
- Initializes connection to App Store or Google Play using OpenIAP-compliant API
- Must be called before any other IAP operations
- Connection state is monitored via `isConnected` StateFlow

### 2. Product Loading (OpenIAP-Compliant)
```kotlin
val products = kmpIapInstance.fetchProducts {
    skus = productIds
    type = ProductQueryType.InApp
}
```
- Fetches products implementing `ProductCommon` interface
- Returns OpenIAP-compliant product objects with unified fields
- Product IDs must be configured in store console

### 3. Purchase Flow (OpenIAP-Compliant)
```kotlin
kmpIapInstance.requestPurchase {
    ios {
        sku = productId
        quantity = 1
        appAccountToken = getUserId()
    }
    android {
        skus = listOf(productId)
        obfuscatedAccountIdAndroid = getUserId()
    }
}
```
- Uses OpenIAP-compliant `RequestPurchaseProps` structure
- Platform-specific options in dedicated iOS/Android properties
- Purchase result comes through `purchaseUpdatedListener` Flow
- Errors are delivered via `purchaseErrorListener` Flow

### 4. Transaction Finishing
```kotlin
val success = kmpIapInstance.finishTransaction(
    purchase = purchase.toPurchaseInput(),
    isConsumable = true // or false for non-consumables
)
```
- Essential for completing the purchase flow
- Handles both iOS and Android transaction completion
- `isConsumable` parameter determines transaction type

### 5. Error Handling
The example demonstrates handling common error scenarios:
- User cancellation (don't show error)
- Network errors (suggest retry)
- Already owned items (suggest restore)
- Generic errors (show user-friendly message)

## Usage Instructions

1. **Replace Product IDs**: Update `productIds` with your actual product IDs
2. **Configure Stores**: 
   - iOS: Add products to App Store Connect
   - Android: Add products to Google Play Console
3. **Implement Server Verification**: Replace `verifyPurchase` with real server validation
4. **Customize Product Delivery**: Update `deliverProduct` with your business logic
5. **Style the UI**: Customize the Compose UI to match your app's design

## Customization Options

### Product Types
```kotlin
// For different product types
enum class ProductType { CONSUMABLE, NON_CONSUMABLE, SUBSCRIPTION }

fun isConsumableProduct(productId: String): Boolean {
    // Your logic to determine consumable products
    return listOf("coins_100", "coins_500").contains(productId)
}
```

### Custom Error Handling
```kotlin
fun handlePurchaseError(error: PurchaseError) {
    when (error.code) {
        ErrorCode.UserCancelled -> { /* Silent */ }
        ErrorCode.NetworkError -> { /* Show retry */ }
        ErrorCode.AlreadyOwned -> { /* Suggest restore */ }
        // Add your custom error handling
    }
}
```

### Loading States
```kotlin
// Add loading indicators for better UX
data class LoadingState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null
)
```

## Next Steps

- **Learn Subscriptions**: Check out the [Subscription Store Example](./subscription-store.md)
- **Advanced Features**: See the [Complete Implementation](./complete-implementation.md)
- **Error Handling**: Read the [Error Codes Reference](../api/error-codes.md)
- **Platform Setup**: Review [iOS Setup](../getting-started/ios-setup.md) and [Android Setup](../getting-started/android-setup.md)