---
sidebar_position: 3
title: Complete Implementation
---


import IapKitBanner from '@site/src/uis/IapKitBanner';

# Complete Production-Ready Implementation

<IapKitBanner />

A comprehensive, production-ready implementation with all best practices for a robust in-app purchase system using Kotlin Multiplatform.

## Architecture Overview

This implementation includes:
- State management with ViewModel and StateFlow
- Server-side receipt validation
- Offline support with local caching
- Comprehensive error handling
- Analytics tracking
- Security best practices
- Dependency injection with Koin

## Complete Store Implementation

### 1. IAP Service

```kotlin
// services/IAPService.kt
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.data.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

interface IAPService {
    val purchaseUpdates: SharedFlow<PurchaseUpdate>
    suspend fun initialize(): Boolean
    suspend fun getProducts(productIds: List<String>): List<Product>
    suspend fun getSubscriptions(subscriptionIds: List<String>): List<Product>
    suspend fun purchaseProduct(productId: String)
    suspend fun purchaseSubscription(productId: String)
    suspend fun getAvailablePurchases(): List<Purchase>
    suspend fun restorePurchases()
    fun dispose()
}

class IAPServiceImpl(
    private val scope: CoroutineScope,
    private val httpClient: HttpClient,
    private val authService: AuthService,
    private val analyticsService: AnalyticsService,
    private val localCache: LocalCache
) : IAPService {
    
    
    private val _purchaseUpdates = MutableSharedFlow<PurchaseUpdate>()
    override val purchaseUpdates: SharedFlow<PurchaseUpdate> = _purchaseUpdates.asSharedFlow()
    
    init {
        observeStates()
    }
    
    override suspend fun initialize(): Boolean {
        return try {
            KmpIAP.initConnection()
            true
        } catch (e: PurchaseError) {
            println("IAP initialization failed: ${e.message}")
            false
        }
    }
    
    private fun observeStates() {
        // Observe purchase updates
        scope.launch {
            KmpIAP.currentPurchase.collectLatest { purchase ->
                purchase?.let { handlePurchaseUpdate(it) }
            }
        }
        
        // Observe errors
        scope.launch {
            KmpIAP.purchaseErrorListener.collect { error ->
                _purchaseUpdates.emit(
                    PurchaseUpdate(
                        item = null,
                        status = PurchaseStatus.ERROR,
                        error = error.message
                    )
                )
            }
        }
    }
    
    private suspend fun handlePurchaseUpdate(purchase: Purchase) {
        try {
            // Validate receipt server-side
            val validationResult = validatePurchase(purchase)
            
            if (validationResult.isValid) {
                // Deliver content
                deliverPurchase(purchase, validationResult)
                
                // Complete transaction
                completeTransaction(purchase)
                
                _purchaseUpdates.emit(
                    PurchaseUpdate(
                        item = purchase,
                        status = PurchaseStatus.SUCCESS,
                        validationResult = validationResult
                    )
                )
                
                // Clear purchase state
                KmpIAP.clearPurchase()
            } else {
                _purchaseUpdates.emit(
                    PurchaseUpdate(
                        item = purchase,
                        status = PurchaseStatus.VALIDATION_FAILED,
                        error = "Receipt validation failed"
                    )
                )
            }
        } catch (e: Exception) {
            _purchaseUpdates.emit(
                PurchaseUpdate(
                    item = purchase,
                    status = PurchaseStatus.ERROR,
                    error = e.message
                )
            )
        }
    }
    
    override suspend fun getProducts(productIds: List<String>): List<Product> {
        return try {
            KmpIAP.getProducts(productIds)
        } catch (e: PurchaseError) {
            println("Failed to get products: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getSubscriptions(subscriptionIds: List<String>): List<Product> {
        return try {
            // In KMP-IAP, subscriptions are also products
            KmpIAP.getProducts(subscriptionIds)
        } catch (e: PurchaseError) {
            println("Failed to get subscriptions: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun purchaseProduct(productId: String) {
        KmpIAP.requestPurchase(
            sku = productId
        )
    }
    
    override suspend fun purchaseSubscription(productId: String) {
        KmpIAP.requestPurchase(sku = productId)
    }
    
    override suspend fun getAvailablePurchases(): List<Purchase> {
        return KmpIAP.availablePurchases.value
    }
    
    override suspend fun restorePurchases() {
        // Purchases are automatically restored via availablePurchases StateFlow
        val purchases = getAvailablePurchases()
        
        // Re-validate and deliver non-consumable purchases
        purchases.forEach { purchase ->
            if (!isConsumableProduct(purchase.productId)) {
                val validationResult = validatePurchase(purchase)
                if (validationResult.isValid) {
                    deliverPurchase(purchase, validationResult)
                }
            }
        }
    }
    
    private suspend fun validatePurchase(purchase: Purchase): ValidationResult {
        return try {
            val platform = when {
                Platform.isIOS -> "ios"
                Platform.isAndroid -> "android"
                else -> "unknown"
            }
            
            val response = httpClient.post("${Config.BASE_URL}/api/validate-purchase") {
                contentType(ContentType.Application.Json)
                bearerAuth(authService.getToken())
                setBody(
                    ValidationRequest(
                        platform = platform,
                        productId = purchase.productId,
                        transactionId = purchase.transactionId,
                        receipt = purchase.transactionReceipt,
                        userId = authService.getCurrentUserId()
                    )
                )
            }
            
            if (response.status == HttpStatusCode.OK) {
                val data: ValidationResponse = response.body()
                ValidationResult.Valid(data.purchaseData)
            } else {
                ValidationResult.Invalid("Server validation failed")
            }
        } catch (e: Exception) {
            ValidationResult.Invalid(e.message ?: "Validation error")
        }
    }
    
    private suspend fun deliverPurchase(
        purchase: Purchase,
        validationResult: ValidationResult
    ) {
        // Update local storage
        localCache.savePurchase(purchase)
        
        // Track analytics
        analyticsService.trackPurchase(purchase)
        
        // Grant content/features based on product ID
        when (purchase.productId) {
            "coins_100" -> localCache.addCoins(100)
            "coins_500" -> localCache.addCoins(500)
            "remove_ads" -> localCache.setPremiumFeature("ads_removed", true)
            "premium_upgrade" -> localCache.setPremiumFeature("premium", true)
            "premium_monthly", "premium_yearly" -> {
                localCache.setActiveSubscription(
                    productId = purchase.productId,
                    purchaseData = validationResult.purchaseData
                )
            }
        }
    }
    
    private suspend fun completeTransaction(purchase: Purchase) {
        val isConsumable = isConsumableProduct(purchase.productId)
        
        val success = KmpIAP.finishTransaction(
            purchase = purchase,
            isConsumable = isConsumable
        )
        
        if (success) {
            println("Transaction completed: ${purchase.productId}")
        } else {
            println("Failed to complete transaction: ${purchase.productId}")
        }
    }
    
    private fun isConsumableProduct(productId: String): Boolean {
        return ProductConfig.isConsumable(productId)
    }
    
    override fun dispose() {
        KmpIAP.dispose()
    }
}

// Data models
data class PurchaseUpdate(
    val item: Purchase?,
    val status: PurchaseStatus,
    val error: String? = null,
    val validationResult: ValidationResult? = null
)

enum class PurchaseStatus {
    SUCCESS,
    ERROR,
    VALIDATION_FAILED,
    CANCELLED
}

sealed class ValidationResult {
    abstract val purchaseData: Map<String, Any>?
    abstract val isValid: Boolean
    
    data class Valid(
        override val purchaseData: Map<String, Any>
    ) : ValidationResult() {
        override val isValid = true
    }
    
    data class Invalid(
        val error: String
    ) : ValidationResult() {
        override val purchaseData = null
        override val isValid = false
    }
}

@Serializable
data class ValidationRequest(
    val platform: String,
    val productId: String,
    val transactionId: String?,
    val receipt: String?,
    val userId: String
)

@Serializable
data class ValidationResponse(
    val isValid: Boolean,
    val purchaseData: Map<String, JsonElement>
)
```

### 2. Store ViewModel

```kotlin
// viewmodels/StoreViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StoreViewModel : ViewModel(), KoinComponent {
    private val iapService: IAPService by inject()
    private val localCache: LocalCache by inject()
    
    data class StoreState(
        val isInitialized: Boolean = false,
        val isLoading: Boolean = false,
        val products: List<Product> = emptyList(),
        val subscriptions: List<Product> = emptyList(),
        val purchases: List<Purchase> = emptyList(),
        val error: String? = null,
        val selectedTab: Int = 0
    )
    
    private val _state = MutableStateFlow(StoreState())
    val state: StateFlow<StoreState> = _state.asStateFlow()
    
    init {
        initialize()
        observePurchaseUpdates()
    }
    
    private fun initialize() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val isInitialized = iapService.initialize()
                
                if (isInitialized) {
                    // Load initial data
                    loadProducts()
                    loadPurchases()
                }
                
                _state.update { 
                    it.copy(
                        isInitialized = isInitialized,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
    
    private fun observePurchaseUpdates() {
        viewModelScope.launch {
            iapService.purchaseUpdates.collectLatest { update ->
                when (update.status) {
                    PurchaseStatus.SUCCESS -> {
                        loadPurchases() // Refresh purchases
                        showMessage("Purchase successful!")
                    }
                    PurchaseStatus.ERROR,
                    PurchaseStatus.VALIDATION_FAILED -> {
                        _state.update { it.copy(error = update.error) }
                    }
                    PurchaseStatus.CANCELLED -> {
                        // Handle cancellation silently
                    }
                }
            }
        }
    }
    
    private suspend fun loadProducts() {
        try {
            val productIds = ProductConfig.getAllProductIds()
            val subscriptionIds = ProductConfig.getAllSubscriptionIds()
            
            val products = iapService.getProducts(productIds)
            val subscriptions = iapService.getSubscriptions(subscriptionIds)
            
            _state.update { 
                it.copy(
                    products = products,
                    subscriptions = subscriptions
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }
    
    private suspend fun loadPurchases() {
        try {
            val purchases = iapService.getAvailablePurchases()
            _state.update { it.copy(purchases = purchases) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }
    
    fun purchaseProduct(productId: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(error = null) }
                iapService.purchaseProduct(productId)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun purchaseSubscription(productId: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(error = null) }
                iapService.purchaseSubscription(productId)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun restorePurchases() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                iapService.restorePurchases()
                showMessage("Purchases restored")
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun isPurchased(productId: String): Boolean {
        return _state.value.purchases.any { it.productId == productId }
    }
    
    fun isSubscriptionActive(productId: String): Boolean {
        val purchase = _state.value.purchases.find { it.productId == productId }
        return purchase != null && isSubscriptionValid(purchase)
    }
    
    private fun isSubscriptionValid(purchase: Purchase): Boolean {
        // Check cached subscription data
        val subscriptionData = localCache.getActiveSubscription()
        return subscriptionData?.productId == purchase.productId &&
               subscriptionData.isValid()
    }
    
    fun selectTab(index: Int) {
        _state.update { it.copy(selectedTab = index) }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun retry() {
        initialize()
    }
    
    private fun showMessage(message: String) {
        // In a real app, emit to a message channel
        println("Store: $message")
    }
    
    override fun onCleared() {
        super.onCleared()
        iapService.dispose()
    }
}
```

### 3. Store UI (Compose)

```kotlin
// ui/screens/StoreScreen.kt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    viewModel: StoreViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store") },
                actions = {
                    IconButton(
                        onClick = viewModel::restorePurchases,
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = "Restore Purchases"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !state.isInitialized -> {
                    InitializingView()
                }
                state.isLoading && state.products.isEmpty() -> {
                    LoadingView()
                }
                state.error != null && state.products.isEmpty() -> {
                    ErrorView(
                        error = state.error!!,
                        onRetry = viewModel::retry
                    )
                }
                else -> {
                    StoreContent(
                        state = state,
                        viewModel = viewModel
                    )
                }
            }
            
            // Error snackbar for non-critical errors
            state.error?.let { error ->
                if (state.products.isNotEmpty()) {
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        action = {
                            TextButton(onClick = viewModel::clearError) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(error)
                    }
                }
            }
        }
    }
}

@Composable
fun InitializingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Initializing store...")
        }
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                "Store Error",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreContent(
    state: StoreViewModel.StoreState,
    viewModel: StoreViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Tab bar
        TabRow(selectedTabIndex = state.selectedTab) {
            Tab(
                selected = state.selectedTab == 0,
                onClick = { viewModel.selectTab(0) },
                text = { Text("Products") }
            )
            Tab(
                selected = state.selectedTab == 1,
                onClick = { viewModel.selectTab(1) },
                text = { Text("Subscriptions") }
            )
        }
        
        // Tab content
        when (state.selectedTab) {
            0 -> ProductsTab(
                products = state.products,
                isPurchased = viewModel::isPurchased,
                onPurchase = viewModel::purchaseProduct
            )
            1 -> SubscriptionsTab(
                subscriptions = state.subscriptions,
                isActive = viewModel::isSubscriptionActive,
                onSubscribe = viewModel::purchaseSubscription
            )
        }
    }
}

@Composable
fun ProductsTab(
    products: List<Product>,
    isPurchased: (String) -> Boolean,
    onPurchase: (String) -> Unit
) {
    if (products.isEmpty()) {
        EmptyStateView("No products available")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products) { product ->
                ProductCard(
                    product = product,
                    isPurchased = isPurchased(product.productId),
                    onPurchase = { onPurchase(product.productId) }
                )
            }
        }
    }
}

@Composable
fun SubscriptionsTab(
    subscriptions: List<Product>,
    isActive: (String) -> Boolean,
    onSubscribe: (String) -> Unit
) {
    if (subscriptions.isEmpty()) {
        EmptyStateView("No subscriptions available")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(subscriptions) { subscription ->
                SubscriptionCard(
                    subscription = subscription,
                    isActive = isActive(subscription.productId),
                    onSubscribe = { onSubscribe(subscription.productId) }
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    isPurchased: Boolean,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.title,
                    style = MaterialTheme.typography.titleMedium
                )
                product.description?.let { desc ->
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isPurchased) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color(0xFF4CAF50),
                    contentColor = Color.White
                ) {
                    Text(
                        "OWNED",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(onClick = onPurchase) {
                    Text(product.price)
                }
            }
        }
    }
}

@Composable
fun SubscriptionCard(
    subscription: Product,
    isActive: Boolean,
    onSubscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        subscription.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
                subscription.description?.let { desc ->
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isActive) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        "ACTIVE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(onClick = onSubscribe) {
                    Text(subscription.price)
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

### 4. Configuration & Security

```kotlin
// config/ProductConfig.kt
object ProductConfig {
    private val products = mapOf(
        "coins_100" to ProductInfo(type = ProductType.CONSUMABLE, value = 100),
        "coins_500" to ProductInfo(type = ProductType.CONSUMABLE, value = 500),
        "remove_ads" to ProductInfo(type = ProductType.NON_CONSUMABLE),
        "premium_monthly" to ProductInfo(type = ProductType.SUBSCRIPTION),
        "premium_yearly" to ProductInfo(type = ProductType.SUBSCRIPTION)
    )
    
    fun getAllProductIds(): List<String> {
        return products.entries
            .filter { it.value.type != ProductType.SUBSCRIPTION }
            .map { it.key }
    }
    
    fun getAllSubscriptionIds(): List<String> {
        return products.entries
            .filter { it.value.type == ProductType.SUBSCRIPTION }
            .map { it.key }
    }
    
    fun isConsumable(productId: String): Boolean {
        return products[productId]?.type == ProductType.CONSUMABLE
    }
    
    fun getProductInfo(productId: String): ProductInfo? {
        return products[productId]
    }
}

data class ProductInfo(
    val type: ProductType,
    val value: Int? = null
)

enum class ProductType {
    CONSUMABLE,
    NON_CONSUMABLE,
    SUBSCRIPTION
}

// config/Config.kt
object Config {
    const val BASE_URL = "https://api.yourapp.com"
    const val API_KEY = BuildConfig.API_KEY // Store in build config
}
```

### 5. Dependency Injection Setup

```kotlin
// di/AppModule.kt
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val appModule = module {
    // Networking
    single {
        HttpClient {
            install(ContentNegotiation) {
                json()
            }
        }
    }
    
    // Coroutine scope for IAP
    single {
        CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
    
    // Services
    single<AuthService> { AuthServiceImpl() }
    single<AnalyticsService> { AnalyticsServiceImpl() }
    single<LocalCache> { LocalCacheImpl(get()) }
    single<IAPService> { 
        IAPServiceImpl(
            scope = get(),
            httpClient = get(),
            authService = get(),
            analyticsService = get(),
            localCache = get()
        )
    }
    
    // ViewModels
    viewModel { StoreViewModel() }
}
```

### 6. Local Cache Implementation

```kotlin
// cache/LocalCache.kt
import com.russhwolf.settings.Settings
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.datetime.*

interface LocalCache {
    suspend fun savePurchase(purchase: Purchase)
    suspend fun getPurchases(): List<Purchase>
    suspend fun addCoins(amount: Int)
    suspend fun getCoins(): Int
    suspend fun setPremiumFeature(feature: String, enabled: Boolean)
    suspend fun isPremiumFeature(feature: String): Boolean
    suspend fun setActiveSubscription(productId: String, purchaseData: Map<String, Any>?)
    suspend fun getActiveSubscription(): SubscriptionData?
}

class LocalCacheImpl(
    private val settings: Settings
) : LocalCache {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun savePurchase(purchase: Purchase) {
        val purchases = getPurchases().toMutableList()
        purchases.removeAll { it.productId == purchase.productId }
        purchases.add(purchase)
        
        val purchasesJson = json.encodeToString(purchases)
        settings.putString("purchases", purchasesJson)
    }
    
    override suspend fun getPurchases(): List<Purchase> {
        val purchasesJson = settings.getStringOrNull("purchases") ?: return emptyList()
        return try {
            json.decodeFromString(purchasesJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun addCoins(amount: Int) {
        val currentCoins = getCoins()
        settings.putInt("coins", currentCoins + amount)
    }
    
    override suspend fun getCoins(): Int {
        return settings.getInt("coins", 0)
    }
    
    override suspend fun setPremiumFeature(feature: String, enabled: Boolean) {
        settings.putBoolean("premium_$feature", enabled)
    }
    
    override suspend fun isPremiumFeature(feature: String): Boolean {
        return settings.getBoolean("premium_$feature", false)
    }
    
    override suspend fun setActiveSubscription(
        productId: String, 
        purchaseData: Map<String, Any>?
    ) {
        val subscriptionData = SubscriptionData(
            productId = productId,
            purchaseTime = Clock.System.now(),
            purchaseData = purchaseData
        )
        
        val dataJson = json.encodeToString(subscriptionData)
        settings.putString("active_subscription", dataJson)
    }
    
    override suspend fun getActiveSubscription(): SubscriptionData? {
        val dataJson = settings.getStringOrNull("active_subscription") ?: return null
        return try {
            json.decodeFromString(dataJson)
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class SubscriptionData(
    val productId: String,
    @Serializable(with = InstantSerializer::class)
    val purchaseTime: Instant,
    val purchaseData: Map<String, @Contextual Any>?
) {
    fun isValid(): Boolean {
        // Implement subscription expiration logic
        // This is a simplified check - in production, verify with server
        val daysSincePurchase = (Clock.System.now() - purchaseTime).inWholeDays
        
        return when {
            productId.contains("monthly") -> daysSincePurchase < 30
            productId.contains("yearly") -> daysSincePurchase < 365
            else -> false
        }
    }
}

@Serializer(forClass = Instant::class)
object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}
```

## Security Best Practices

1. **Server-Side Validation**: All receipts validated on secure backend
2. **User Authentication**: Purchases tied to authenticated user accounts
3. **Secure Storage**: Purchase data encrypted using platform secure storage
4. **Network Security**: HTTPS only with certificate pinning
5. **Code Obfuscation**: Use R8/ProGuard for Android, Swift obfuscation for iOS

## Production Considerations

### Error Handling & Recovery

```kotlin
// Add comprehensive error recovery
class ErrorRecoveryHandler {
    suspend fun handlePurchaseError(error: PurchaseError): RecoveryAction {
        return when (error.code) {
            ErrorCode.NETWORK_ERROR -> RecoveryAction.Retry(delay = 2000)
            ErrorCode.SERVICE_UNAVAILABLE -> RecoveryAction.Retry(delay = 5000)
            ErrorCode.PRODUCT_ALREADY_OWNED -> RecoveryAction.RestorePurchases
            else -> RecoveryAction.ShowError(error.message)
        }
    }
}

sealed class RecoveryAction {
    data class Retry(val delay: Long) : RecoveryAction()
    object RestorePurchases : RecoveryAction()
    data class ShowError(val message: String) : RecoveryAction()
}
```

### Analytics Integration

```kotlin
interface AnalyticsService {
    fun trackPurchase(purchase: Purchase)
    fun trackPurchaseError(error: PurchaseError)
    fun trackRestoreCompleted(count: Int)
}

class AnalyticsServiceImpl : AnalyticsService {
    override fun trackPurchase(purchase: Purchase) {
        // Track with your analytics provider
        trackEvent("purchase_completed", mapOf(
            "product_id" to purchase.productId,
            "price" to purchase.price,
            "currency" to purchase.currency
        ))
    }
    
    override fun trackPurchaseError(error: PurchaseError) {
        trackEvent("purchase_error", mapOf(
            "error_code" to error.code.name,
            "error_message" to error.message
        ))
    }
    
    override fun trackRestoreCompleted(count: Int) {
        trackEvent("restore_completed", mapOf(
            "restored_count" to count
        ))
    }
    
    private fun trackEvent(name: String, params: Map<String, Any?>) {
        // Implementation depends on your analytics provider
    }
}
```

### Testing Strategy

```kotlin
// Create test doubles for IAP testing
class MockIAPService : IAPService {
    private val _purchaseUpdates = MutableSharedFlow<PurchaseUpdate>()
    override val purchaseUpdates = _purchaseUpdates.asSharedFlow()
    
    override suspend fun initialize(): Boolean = true
    
    override suspend fun getProducts(productIds: List<String>): List<Product> {
        return productIds.map { id ->
            Product(
                productId = id,
                title = "Test $id",
                description = "Test description",
                price = "$0.99",
                currency = "USD"
            )
        }
    }
    
    // Implement other methods for testing
}
```

## Platform-Specific Considerations

### iOS

```kotlin
// iOS-specific configuration
actual class PlatformConfig {
    actual fun configurePlatform() {
        // Configure StoreKit
        if (isDebugBuild()) {
            // Use StoreKit testing configuration
            configureStoreKitTesting()
        }
    }
}
```

### Android

```kotlin
// Android-specific configuration
actual class PlatformConfig {
    actual fun configurePlatform() {
        // Configure Google Play Billing
        if (BuildConfig.DEBUG) {
            // Enable debug logging
            enableBillingDebugLogging()
        }
    }
}
```

This implementation provides a robust, production-ready foundation for in-app purchases in Kotlin Multiplatform applications with comprehensive error handling, security, and best practices.
