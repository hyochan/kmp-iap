package io.github.hyochan.kmpiap

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AlternativeBillingOnlyReportingDetails
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ExternalOfferReportingDetails
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.UserChoiceBillingListener
import io.github.hyochan.kmpiap.clearProductCache
import io.github.hyochan.kmpiap.ensureConnectedOrFail
import io.github.hyochan.kmpiap.openiap.ActiveSubscription
import io.github.hyochan.kmpiap.openiap.AlternativeBillingModeAndroid
import io.github.hyochan.kmpiap.ConnectionResult
import io.github.hyochan.kmpiap.openiap.AndroidSubscriptionOfferInput
import io.github.hyochan.kmpiap.openiap.DeepLinkOptions
import io.github.hyochan.kmpiap.openiap.ErrorCode
import io.github.hyochan.kmpiap.openiap.ExternalPurchaseLinkResultIOS
import io.github.hyochan.kmpiap.openiap.ExternalPurchaseNoticeResultIOS
import io.github.hyochan.kmpiap.openiap.InitConnectionConfig
import io.github.hyochan.kmpiap.openiap.UserChoiceBillingDetails
import io.github.hyochan.kmpiap.openiap.FetchProductsResult
import io.github.hyochan.kmpiap.openiap.FetchProductsResultProducts
import io.github.hyochan.kmpiap.openiap.FetchProductsResultSubscriptions
import io.github.hyochan.kmpiap.openiap.MutationDeepLinkToSubscriptionsHandler
import io.github.hyochan.kmpiap.openiap.MutationEndConnectionHandler
import io.github.hyochan.kmpiap.openiap.MutationInitConnectionHandler
import io.github.hyochan.kmpiap.openiap.MutationFinishTransactionHandler
import io.github.hyochan.kmpiap.openiap.MutationRequestPurchaseHandler
import io.github.hyochan.kmpiap.openiap.MutationValidateReceiptHandler
import io.github.hyochan.kmpiap.openiap.MutationHandlers
import io.github.hyochan.kmpiap.openiap.Product
import io.github.hyochan.kmpiap.openiap.ProductQueryType
import io.github.hyochan.kmpiap.openiap.ProductRequest
import io.github.hyochan.kmpiap.openiap.Purchase
import io.github.hyochan.kmpiap.openiap.PurchaseAndroid
import io.github.hyochan.kmpiap.openiap.IapPlatform
import io.github.hyochan.kmpiap.openiap.ProductIOS
import io.github.hyochan.kmpiap.openiap.PurchaseError
import io.github.hyochan.kmpiap.openiap.PurchaseOptions
import io.github.hyochan.kmpiap.openiap.QueryFetchProductsHandler
import io.github.hyochan.kmpiap.openiap.QueryGetActiveSubscriptionsHandler
import io.github.hyochan.kmpiap.openiap.QueryGetAvailablePurchasesHandler
import io.github.hyochan.kmpiap.openiap.QueryHasActiveSubscriptionsHandler
import io.github.hyochan.kmpiap.openiap.QueryHandlers
import io.github.hyochan.kmpiap.openiap.RequestPurchaseProps
import io.github.hyochan.kmpiap.openiap.RequestPurchaseResult
import io.github.hyochan.kmpiap.openiap.RequestPurchaseResultPurchase
import io.github.hyochan.kmpiap.openiap.RequestPurchaseResultPurchases
import io.github.hyochan.kmpiap.openiap.AppTransaction
import io.github.hyochan.kmpiap.openiap.SubscriptionStatusIOS
import io.github.hyochan.kmpiap.openiap.PurchaseInput
import io.github.hyochan.kmpiap.openiap.SubscriptionHandlers
import io.github.hyochan.kmpiap.Store
import io.github.hyochan.kmpiap.PurchaseException
import io.github.hyochan.kmpiap.openiap.VerifyPurchaseWithProviderProps
import io.github.hyochan.kmpiap.openiap.VerifyPurchaseWithProviderResult
import io.github.hyochan.kmpiap.openiap.VerifyPurchaseProps
import io.github.hyochan.kmpiap.openiap.VerifyPurchaseResult
import io.github.hyochan.kmpiap.openiap.VerifyPurchaseResultAndroid
import io.github.hyochan.kmpiap.openiap.VerifyPurchaseResultIOS
import io.github.hyochan.kmpiap.openiap.PurchaseIOS
import io.github.hyochan.kmpiap.openiap.PurchaseVerificationProvider
import io.github.hyochan.kmpiap.openiap.RequestVerifyPurchaseWithIapkitResult
import io.github.hyochan.kmpiap.openiap.IapStore
import io.github.hyochan.kmpiap.openiap.IapkitPurchaseState
import dev.hyo.openiap.RequestVerifyPurchaseWithIapkitProps as GoogleVerifyPurchaseWithIapkitProps
import dev.hyo.openiap.RequestVerifyPurchaseWithIapkitGoogleProps as GoogleVerifyPurchaseWithIapkitGoogleProps
import dev.hyo.openiap.utils.verifyPurchaseWithIapkit as verifyPurchaseWithIapkitGoogle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.collections.buildList
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class InAppPurchaseAndroid : KmpInAppPurchase, Application.ActivityLifecycleCallbacks {

    private var billingClient: BillingClient? = null
    private var isConnected = false
    private var context: Context? = null
    private var currentActivity: Activity? = null
    private var activityCallbacksDisposer: (() -> Unit)? = null
    private val cachedProductDetails = ConcurrentHashMap<String, ProductDetails>()
    private var currentPurchaseCallback: ((Result<List<Purchase>>) -> Unit)? = null
    private var alternativeBillingMode: AlternativeBillingModeAndroid = AlternativeBillingModeAndroid.None

    // ---------------------------------------------------------------------
    // Event streams
    // ---------------------------------------------------------------------
    private val _purchaseUpdatedListener = MutableSharedFlow<Purchase>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val purchaseUpdatedListener: Flow<Purchase> = _purchaseUpdatedListener.asSharedFlow()

    private val _purchaseErrorListener = MutableSharedFlow<PurchaseError>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val purchaseErrorListener: Flow<PurchaseError> = _purchaseErrorListener.asSharedFlow()

    private val _connectionStateListener = MutableSharedFlow<ConnectionResult>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val connectionStateListener: Flow<ConnectionResult> = _connectionStateListener.asSharedFlow()

    private val _promotedProductListener = MutableSharedFlow<String?>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val promotedProductListener: Flow<String?> = _promotedProductListener.asSharedFlow()

    private val _userChoiceBillingListener = MutableSharedFlow<UserChoiceBillingDetails>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val userChoiceBillingListener: Flow<UserChoiceBillingDetails> = _userChoiceBillingListener.asSharedFlow()

    private fun failWith(error: PurchaseError): Nothing =
        emitFailureAndThrow(_purchaseErrorListener, error)

    private fun mapFetchResultToProducts(
        params: ProductRequest,
        result: FetchProductsResult
    ): List<Product> = mapFetchResultToProductsHelper(params, result, cachedProductDetails)

    // ---------------------------------------------------------------------
    // Mutation handlers
    // ---------------------------------------------------------------------
    private val initConnectionHandler: MutationInitConnectionHandler = { config ->
        withContext(Dispatchers.IO) {
            // Set alternative billing mode (reset to None if no config supplied)
            alternativeBillingMode = config?.alternativeBillingModeAndroid ?: AlternativeBillingModeAndroid.None

            if (context == null) {
                val disposer = tryCaptureApplication(
                    callback = this@InAppPurchaseAndroid,
                    onContextAvailable = { appContext -> context = appContext },
                    onActivityFound = { activity -> currentActivity = activity }
                )
                if (context != null) {
                    activityCallbacksDisposer = disposer
                } else {
                    disposer?.invoke()
                }
            }

            val ctx = context ?: run {
                activityCallbacksDisposer = null
                failWith(
                    PurchaseError(code = ErrorCode.ServiceError, message = "Context not available")
                )
            }

            withTimeout(15_000) {
                suspendCancellableCoroutine { continuation ->
                    val listener = object : BillingClientStateListener {
                        override fun onBillingSetupFinished(result: BillingResult) {
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                isConnected = true
                                _connectionStateListener.tryEmit(ConnectionResult(connected = true, message = "Connected"))
                                continuation.resume(true)
                            } else {
                                val error = PurchaseError(
                                    code = ErrorCode.ServiceError,
                                    message = result.debugMessage ?: "Failed to connect"
                                )
                                _purchaseErrorListener.tryEmit(error)
                                continuation.resume(false)
                            }
                        }

                        override fun onBillingServiceDisconnected() {
                            isConnected = false
                            _connectionStateListener.tryEmit(ConnectionResult(connected = false, message = "Disconnected"))
                        }
                    }

                    val builder = BillingClient.newBuilder(ctx)
                        .setListener { billingResult, purchases ->
                            handlePurchaseUpdate(billingResult, purchases)
                        }

                    // Configure alternative billing if specified
                    when (alternativeBillingMode) {
                        AlternativeBillingModeAndroid.UserChoice -> {
                            builder.enableUserChoiceBilling { userChoiceDetails ->
                                val details = UserChoiceBillingDetails(
                                    externalTransactionToken = userChoiceDetails.externalTransactionToken,
                                    products = userChoiceDetails.products.map { it.id }
                                )
                                _userChoiceBillingListener.tryEmit(details)
                            }
                        }
                        AlternativeBillingModeAndroid.AlternativeOnly -> {
                            builder.enableAlternativeBillingOnly()
                        }
                        AlternativeBillingModeAndroid.None -> {
                            // Standard billing, no changes needed
                        }
                    }

                    billingClient = enablePendingPurchasesCompat(builder).build()
                    billingClient?.startConnection(listener)

                    continuation.invokeOnCancellation {
                        billingClient?.endConnection()
                        billingClient = null
                    }
                }
            }
        }
    }

    private val endConnectionHandler: MutationEndConnectionHandler = {
        withContext(Dispatchers.IO) {
            runCatching {
                billingClient?.endConnection()
                billingClient = null
                isConnected = false
                activityCallbacksDisposer?.invoke()
                activityCallbacksDisposer = null
                _connectionStateListener.tryEmit(ConnectionResult(connected = false, message = "Disconnected"))
                clearProductCache(cachedProductDetails)
                true
            }.getOrElse { false }
        }
    }

    private val requestPurchaseHandler: MutationRequestPurchaseHandler = { props ->
        val purchases = withContext(Dispatchers.Main) {
            val resolvedType = props.type ?: when (props.request) {
                is RequestPurchaseProps.Request.Purchase -> ProductQueryType.InApp
                is RequestPurchaseProps.Request.Subscription -> ProductQueryType.Subs
            }

            val purchaseAndroidOptions = (props.request as? RequestPurchaseProps.Request.Purchase)?.value?.android
            val subscriptionAndroidOptions = (props.request as? RequestPurchaseProps.Request.Subscription)?.value?.android

            val subscriptionOffers: List<AndroidSubscriptionOfferInput> =
                subscriptionAndroidOptions?.subscriptionOffers.orEmpty()

            val purchaseTokenAndroid = subscriptionAndroidOptions?.purchaseTokenAndroid
            val replacementModeAndroid = subscriptionAndroidOptions?.replacementModeAndroid

            val targetSkus: List<String> =
                purchaseAndroidOptions?.skus ?: subscriptionAndroidOptions?.skus ?: emptyList()

            val isOfferPersonalized = purchaseAndroidOptions?.isOfferPersonalized
                ?: subscriptionAndroidOptions?.isOfferPersonalized
            val obfuscatedAccountIdAndroid = purchaseAndroidOptions?.obfuscatedAccountIdAndroid
                ?: subscriptionAndroidOptions?.obfuscatedAccountIdAndroid
            val obfuscatedProfileIdAndroid = purchaseAndroidOptions?.obfuscatedProfileIdAndroid
                ?: subscriptionAndroidOptions?.obfuscatedProfileIdAndroid
            if (targetSkus.isEmpty()) {
                _purchaseErrorListener.tryEmit(
                    PurchaseError(code = ErrorCode.EmptySkuList, message = "SKU list is empty")
                )
                return@withContext emptyList()
            }

            val activity = currentActivity
            if (activity == null) {
                _purchaseErrorListener.tryEmit(
                    PurchaseError(code = ErrorCode.ActivityUnavailable, message = "Activity not available for purchase")
                )
                return@withContext emptyList()
            }

            val client = billingClient
            if (client == null || !client.isReady) {
                _purchaseErrorListener.tryEmit(
                    PurchaseError(code = ErrorCode.NotPrepared, message = "Billing client not ready")
                )
                return@withContext emptyList()
            }

            val desiredProductType =
                if (resolvedType == ProductQueryType.Subs) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP

            val productDetailsBySku = loadProductDetails(
                client = client,
                productType = desiredProductType,
                skus = targetSkus,
                cache = cachedProductDetails,
                errorFlow = _purchaseErrorListener
            )
                ?: return@withContext emptyList()

            suspendCancellableCoroutine<List<Purchase>> { continuation ->
                currentPurchaseCallback = { result ->
                    if (continuation.isActive) continuation.resume(result.getOrDefault(emptyList()))
                }

                val paramsList = mutableListOf<BillingFlowParams.ProductDetailsParams>()
                val offersBySku = subscriptionOffers
                    .groupBy(AndroidSubscriptionOfferInput::sku)
                    .mapValues { entry -> entry.value.toMutableList() }
                    .toMutableMap()

                var mismatch = false
                for (sku in targetSkus) {
                    val detail = productDetailsBySku[sku] ?: continue
                    val builder = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(detail)

                    if (desiredProductType == BillingClient.ProductType.SUBS) {
                        val availableTokens = detail.subscriptionOfferDetails?.map { it.offerToken }.orEmpty()
                        val queuedToken = offersBySku[detail.productId]?.takeIf { it.isNotEmpty() }?.removeAt(0)?.offerToken
                        val resolvedToken = queuedToken ?: detail.subscriptionOfferDetails?.firstOrNull()?.offerToken

                        if (resolvedToken.isNullOrEmpty() || (availableTokens.isNotEmpty() && !availableTokens.contains(resolvedToken))) {
                            _purchaseErrorListener.tryEmit(
                                PurchaseError(
                                    code = ErrorCode.SkuOfferMismatch,
                                    message = "Offer token mismatch for ${detail.productId}"
                                )
                            )
                            continuation.resume(emptyList())
                            currentPurchaseCallback = null
                            mismatch = true
                            break
                        }

                        builder.setOfferToken(resolvedToken)
                    }

                    paramsList += builder.build()
                }

                if (mismatch) return@suspendCancellableCoroutine

                val flowBuilder = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(paramsList)

                if (isOfferPersonalized == true) {
                    flowBuilder.setIsOfferPersonalized(true)
                }
                obfuscatedAccountIdAndroid?.let { accountId ->
                    flowBuilder.setObfuscatedAccountId(accountId)
                }
                obfuscatedProfileIdAndroid?.let { profileId ->
                    flowBuilder.setObfuscatedProfileId(profileId)
                }

                if (desiredProductType == BillingClient.ProductType.SUBS && !purchaseTokenAndroid.isNullOrEmpty()) {
                    val updateParamsBuilder = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(purchaseTokenAndroid)
                    replacementModeAndroid?.let(updateParamsBuilder::setSubscriptionReplacementMode)
                    flowBuilder.setSubscriptionUpdateParams(updateParamsBuilder.build())
                }

                val launchResult = billingClient?.launchBillingFlow(activity, flowBuilder.build())
                if (launchResult?.responseCode != BillingClient.BillingResponseCode.OK) {
                    val error = PurchaseError(
                        code = mapBillingResponseCode(launchResult?.responseCode ?: -1),
                        message = launchResult?.debugMessage ?: "Failed to launch billing flow"
                    )
                    _purchaseErrorListener.tryEmit(error)
                    currentPurchaseCallback?.invoke(Result.success(emptyList()))
                    currentPurchaseCallback = null
                }

                continuation.invokeOnCancellation { currentPurchaseCallback = null }
            }
        }

        RequestPurchaseResultPurchases(purchases)
    }

    private val validateReceiptHandler: MutationValidateReceiptHandler = { _ ->
        // Android doesn't support native receipt validation like iOS
        // Use verifyPurchaseWithProvider for server-side verification
        failWith(
            PurchaseError(
                code = ErrorCode.FeatureNotSupported,
                message = "validateReceipt is not supported on Android. Use verifyPurchaseWithProvider for server-side verification."
            )
        )
    }

    private val deepLinkToSubscriptionsHandler: MutationDeepLinkToSubscriptionsHandler = { options ->
        options?.let { launchDeepLinkToSubscriptions(it) }
    }

    private val finishTransactionHandler: MutationFinishTransactionHandler = finishTransaction@{ purchase, isConsumable ->
        if (purchase.platform != IapPlatform.Android) return@finishTransaction
        val token = purchase.purchaseToken ?: return@finishTransaction
        runCatching {
            if (isConsumable == true) {
                consumePurchaseAndroid(token)
            } else {
                acknowledgePurchaseAndroid(token)
            }
        }.onFailure {
            _purchaseErrorListener.tryEmit(
                PurchaseError(
                    code = ErrorCode.ReceiptFinishedFailed,
                    message = "Failed to finish transaction: ${it.message ?: "unknown"}"
                )
            )
        }
    }

    // ---------------------------------------------------------------------
    // Interface implementation
    // ---------------------------------------------------------------------
    override suspend fun initConnection(config: InitConnectionConfig?): Boolean = initConnectionHandler(config)

    override suspend fun endConnection(): Boolean = endConnectionHandler()

    override suspend fun fetchProducts(params: ProductRequest): FetchProductsResult =
        fetchProductsHandler(params)

    override suspend fun requestPurchase(request: RequestPurchaseProps): RequestPurchaseResult? =
        requestPurchaseHandler(request)

    override suspend fun getAvailablePurchases(options: PurchaseOptions?): List<Purchase> =
        getAvailablePurchasesHandler(options)

    suspend fun getPurchaseHistories(options: PurchaseOptions?): List<Purchase> = emptyList()

    override suspend fun getActiveSubscriptions(subscriptionIds: List<String>?): List<ActiveSubscription> =
        getActiveSubscriptionsHandler(subscriptionIds)

    override suspend fun hasActiveSubscriptions(subscriptionIds: List<String>?): Boolean =
        hasActiveSubscriptionsHandler(subscriptionIds)

    override suspend fun restorePurchases() {
        getAvailablePurchasesHandler.invoke(null)
    }

    override suspend fun currentEntitlementIOS(sku: String): PurchaseIOS? = null

    override suspend fun getAppTransactionIOS(): AppTransaction? = null

    override suspend fun getPendingTransactionsIOS(): List<PurchaseIOS> = emptyList()

    override suspend fun getReceiptDataIOS(): String? = null

    override suspend fun getTransactionJwsIOS(sku: String): String? = null

    override suspend fun isEligibleForIntroOfferIOS(groupID: String): Boolean = false

    override suspend fun isTransactionVerifiedIOS(sku: String): Boolean = false

    override suspend fun latestTransactionIOS(sku: String): PurchaseIOS? = null

    override suspend fun subscriptionStatusIOS(sku: String): List<SubscriptionStatusIOS> = emptyList()

    override suspend fun validateReceiptIOS(options: VerifyPurchaseProps): VerifyPurchaseResultIOS {
        failWith(
            PurchaseError(
                code = ErrorCode.FeatureNotSupported,
                message = "validateReceiptIOS is an iOS-only API. Use verifyPurchaseWithProvider for server-side verification on Android."
            )
        )
    }

    suspend fun isPurchaseValid(purchase: Purchase): Boolean = isPurchaseTokenValid(purchase)

    override suspend fun promotedProductIOS(): String = ""

    override suspend fun purchaseError(): PurchaseError = purchaseErrorListener.first()

    override suspend fun purchaseUpdated(): Purchase = purchaseUpdatedListener.first()

    override suspend fun finishTransaction(purchase: PurchaseInput, isConsumable: Boolean?) {
        finishTransactionHandler(purchase, isConsumable)
    }

    override suspend fun deepLinkToSubscriptions(options: DeepLinkOptions?) {
        deepLinkToSubscriptionsHandler(options)
    }

    // ---------------------------------------------------------------------
    // Query handlers
    // ---------------------------------------------------------------------
    private val fetchProductsHandler: QueryFetchProductsHandler = { params ->
        withContext(Dispatchers.IO) {
            val client = billingClient ?: failWith(
                PurchaseError(code = ErrorCode.NotPrepared, message = "Billing client not initialized")
            )
            if (!client.isReady) failWith(
                PurchaseError(code = ErrorCode.NotPrepared, message = "Billing client not ready")
            )
            if (params.skus.isEmpty()) failWith(
                PurchaseError(code = ErrorCode.EmptySkuList, message = "SKU list is empty")
            )

            val queryType = params.type ?: ProductQueryType.All
            val includeInApp = queryType == ProductQueryType.InApp || queryType == ProductQueryType.All
            val includeSubs = queryType == ProductQueryType.Subs || queryType == ProductQueryType.All

            suspend fun query(productType: String): List<ProductDetails> {
                val ordered = mutableListOf<ProductDetails>()
                val missing = mutableListOf<String>()

                params.skus.forEach { sku ->
                    val cached = cachedProductDetails[sku]
                    if (cached != null && cached.productType == productType) {
                        ordered += cached
                    } else {
                        missing += sku
                    }
                }

                if (missing.isNotEmpty()) {
                    val queryParams = QueryProductDetailsParams.newBuilder()
                        .setProductList(
                            missing.map { sku ->
                                QueryProductDetailsParams.Product.newBuilder()
                                    .setProductId(sku)
                                    .setProductType(productType)
                                    .build()
                            }
                        )
                        .build()

                    val queried = suspendCancellableCoroutine<List<ProductDetails>> { continuation ->
                        client.queryProductDetailsAsync(queryParams) { billingResult: BillingResult, result: QueryProductDetailsResult ->
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                result.productDetailsList?.forEach { detail -> cachedProductDetails[detail.productId] = detail }
                                continuation.resume(result.productDetailsList.orEmpty())
                            } else {
                                continuation.resume(emptyList())
                            }
                        }
                    }

                    missing.forEach { sku ->
                        cachedProductDetails[sku]?.takeIf { it.productType == productType }?.let { ordered += it }
                    }

                    queried.filter { detail -> detail.productType == productType && detail.productId in params.skus }
                        .forEach { detail ->
                            if (!ordered.contains(detail)) ordered += detail
                        }
                }

                return ordered
            }

            val inAppDetails = if (includeInApp) query(BillingClient.ProductType.INAPP) else emptyList()
            val subsDetails = if (includeSubs) query(BillingClient.ProductType.SUBS) else emptyList()

            return@withContext when (queryType) {
                ProductQueryType.InApp -> FetchProductsResultProducts(inAppDetails.map { it.toProduct() })
                ProductQueryType.Subs -> FetchProductsResultSubscriptions(subsDetails.mapNotNull { it.toSubscriptionProduct() })
                ProductQueryType.All -> {
                    val combined = buildList<Product> {
                        addAll(inAppDetails.map { it.toProduct() })
                        addAll(subsDetails.map { it.toProduct() })
                    }
                    FetchProductsResultProducts(combined)
                }
            }
        }
    }

    private val getAvailablePurchasesHandler: QueryGetAvailablePurchasesHandler = { _ ->
        withContext(Dispatchers.IO) {
            ensureConnectedOrFail(isConnected, ::failWith)
            val client = billingClient ?: return@withContext emptyList()

            suspend fun query(type: String): List<Purchase> = suspendCancellableCoroutine { continuation ->
                val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
                client.queryPurchasesAsync(params) { result, purchases ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(purchases.map { it.toPurchase() })
                    } else {
                        continuation.resume(emptyList())
                    }
                }
            }

            val all = mutableListOf<Purchase>()
            all += query(BillingClient.ProductType.INAPP)
            all += query(BillingClient.ProductType.SUBS)
            all
        }
    }

    private val getActiveSubscriptionsHandler: QueryGetActiveSubscriptionsHandler = { ids ->
        withContext(Dispatchers.IO) {
            ensureConnectedOrFail(isConnected, ::failWith)
            val client = billingClient ?: return@withContext emptyList()

            suspendCancellableCoroutine { continuation ->
                val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
                client.queryPurchasesAsync(params) { result, purchases ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        val active = purchases
                            .filter { purchase -> ids?.let { list -> purchase.products.any(list::contains) } ?: true }
                            .filter { purchase -> purchase.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED }
                            .map { purchase ->
                                ActiveSubscription(
                                    autoRenewingAndroid = purchase.isAutoRenewing,
                                    isActive = true,
                                    productId = purchase.products.firstOrNull().orEmpty(),
                                    purchaseToken = purchase.purchaseToken,
                                    transactionDate = purchase.purchaseTime.toDouble() / 1000,
                                    transactionId = purchase.orderId ?: purchase.purchaseToken,
                                    willExpireSoon = null,
                                    daysUntilExpirationIOS = null,
                                    environmentIOS = null,
                                    expirationDateIOS = null
                                )
                            }
                        continuation.resume(active)
                    } else {
                        continuation.resume(emptyList())
                    }
                }
            }
        }
    }

    private val hasActiveSubscriptionsHandler: QueryHasActiveSubscriptionsHandler = { ids ->
        withContext(Dispatchers.IO) { getActiveSubscriptionsHandler(ids).isNotEmpty() }
    }

    // ---------------------------------------------------------------------
    // Handler collections
    // ---------------------------------------------------------------------
    val queryHandlers: QueryHandlers by lazy {
        QueryHandlers(
            fetchProducts = fetchProductsHandler,
            getAvailablePurchases = getAvailablePurchasesHandler,
            getActiveSubscriptions = getActiveSubscriptionsHandler,
            hasActiveSubscriptions = hasActiveSubscriptionsHandler
        )
    }

    val mutationHandlers: MutationHandlers by lazy {
        MutationHandlers(
            initConnection = initConnectionHandler,
            endConnection = endConnectionHandler,
            requestPurchase = requestPurchaseHandler,
            validateReceipt = validateReceiptHandler,
            deepLinkToSubscriptions = deepLinkToSubscriptionsHandler,
            finishTransaction = finishTransactionHandler,
            acknowledgePurchaseAndroid = { token -> acknowledgePurchaseAndroid(token) },
            consumePurchaseAndroid = { token -> consumePurchaseAndroid(token) },
            restorePurchases = { getAvailablePurchasesHandler.invoke(null) }
        )
    }

    val subscriptionHandlers: SubscriptionHandlers by lazy {
        SubscriptionHandlers(
            purchaseUpdated = { purchaseUpdatedListener.first() },
            purchaseError = { purchaseErrorListener.first() }
        )
    }

    // ---------------------------------------------------------------------
    // Android specific overrides
    // ---------------------------------------------------------------------
    override suspend fun acknowledgePurchaseAndroid(purchaseToken: String): Boolean {
        ensureConnectedOrFail(isConnected, ::failWith)
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient?.acknowledgePurchase(params) { result ->
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK,
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> continuation.resume(true)
                    else -> {
                        val error = PurchaseError(
                            code = ErrorCode.ServiceError,
                            message = "Failed to acknowledge: ${result.debugMessage} (code: ${result.responseCode})"
                        )
                        _purchaseErrorListener.tryEmit(error)
                        continuation.resumeWithException(PurchaseException(error))
                    }
                }
            }
        }
    }

    override suspend fun consumePurchaseAndroid(purchaseToken: String): Boolean {
        ensureConnectedOrFail(isConnected, ::failWith)
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient?.consumeAsync(params) { result, _ ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(true)
                } else {
                    val error = PurchaseError(
                        code = ErrorCode.ServiceError,
                        message = "Failed to consume: ${result.debugMessage} (code: ${result.responseCode})"
                    )
                    _purchaseErrorListener.tryEmit(error)
                    continuation.resumeWithException(PurchaseException(error))
                }
            }
        }
    }

    override suspend fun getStorefrontIOS(): String = ""
    override suspend fun presentCodeRedemptionSheetIOS(): Boolean = false
    suspend fun finishTransactionIOS(transactionId: String) {}
    override suspend fun clearTransactionIOS(): Boolean = false
    suspend fun clearProductsIOS() {}
    override suspend fun getPromotedProductIOS(): ProductIOS? = null
    override suspend fun requestPurchaseOnPromotedProductIOS(): Boolean = false
    override suspend fun beginRefundRequestIOS(sku: String): String? = null
    override suspend fun showManageSubscriptionsIOS(): List<PurchaseIOS> = emptyList()
    override suspend fun syncIOS(): Boolean = false
    override suspend fun validateReceipt(options: ValidationOptions): ValidationResult = validateReceiptHandler(options)

    override suspend fun verifyPurchase(options: VerifyPurchaseProps): VerifyPurchaseResult {
        // Android doesn't support native receipt verification like iOS
        // Use verifyPurchaseWithProvider for server-side verification via IAPKit
        failWith(
            PurchaseError(
                code = ErrorCode.FeatureNotSupported,
                message = "verifyPurchase is not supported on Android. Use verifyPurchaseWithProvider for server-side verification via IAPKit."
            )
        )
    }

    override suspend fun verifyPurchaseWithProvider(options: VerifyPurchaseWithProviderProps): VerifyPurchaseWithProviderResult {
        if (options.provider != PurchaseVerificationProvider.Iapkit) {
            failWith(
                PurchaseError(
                    code = ErrorCode.FeatureNotSupported,
                    message = "Verification provider ${options.provider.rawValue} is not supported on Android"
                )
            )
        }

        val iapkitOptions = options.iapkit ?: failWith(
            PurchaseError(
                code = ErrorCode.PurchaseVerificationFailed,
                message = "IAPKit options are required for Android verification"
            )
        )
        val googleOptions = iapkitOptions.google ?: failWith(
            PurchaseError(
                code = ErrorCode.PurchaseVerificationFailed,
                message = "Google purchaseToken is required for Android verification"
            )
        )

        return try {
            val openIapProps = GoogleVerifyPurchaseWithIapkitProps(
                apiKey = iapkitOptions.apiKey,
                apple = null,
                google = GoogleVerifyPurchaseWithIapkitGoogleProps(
                    purchaseToken = googleOptions.purchaseToken
                )
            )

            val googleResult = verifyPurchaseWithIapkitGoogle(openIapProps, "kmp-iap-android")

            val iapkitResult = RequestVerifyPurchaseWithIapkitResult(
                isValid = googleResult.isValid,
                state = IapkitPurchaseState.fromJson(googleResult.state.toJson()),
                store = IapStore.fromJson(googleResult.store.toJson())
            )

            VerifyPurchaseWithProviderResult(
                iapkit = iapkitResult,
                provider = options.provider
            )
        } catch (e: Exception) {
            failWith(
                PurchaseError(
                    code = ErrorCode.PurchaseVerificationFailed,
                    message = e.message ?: "Purchase verification failed"
                )
            )
        }
    }

    override fun getVersion(): String = ANDROID_VERSION
    override fun getStore(): Store = Store.PLAY_STORE
    override suspend fun canMakePayments(): Boolean = true

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
    private fun handlePurchaseUpdate(
        billingResult: BillingResult,
        purchases: List<com.android.billingclient.api.Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val mapped = purchases.orEmpty().map { it.toPurchase() }
            mapped.forEach { _purchaseUpdatedListener.tryEmit(it) }
            currentPurchaseCallback?.invoke(Result.success(mapped))
        } else {
            val error = PurchaseError(
                code = mapBillingResponseCode(billingResult.responseCode),
                message = billingResult.debugMessage ?: "Purchase failed"
            )
            _purchaseErrorListener.tryEmit(error)
            currentPurchaseCallback?.invoke(Result.success(emptyList()))
        }
        currentPurchaseCallback = null
    }

    private fun launchDeepLinkToSubscriptions(options: DeepLinkOptions) {
        val sku = options.skuAndroid ?: return
        val activity = currentActivity ?: return
        val url = "https://play.google.com/store/account/subscriptions?sku=$sku&package=${activity.packageName}"
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override suspend fun getStorefront(): String {
        // Android doesn't have a storefront concept like iOS
        // Return a default value or country code based on locale
        return java.util.Locale.getDefault().country
    }

    // ---------------------------------------------------------------------
    // iOS External Purchase Methods (stubs for Android)
    // ---------------------------------------------------------------------

    override suspend fun presentExternalPurchaseLinkIOS(url: String): ExternalPurchaseLinkResultIOS {
        failWith(PurchaseError(code = ErrorCode.FeatureNotSupported, message = "External purchase links are iOS only"))
    }

    override suspend fun presentExternalPurchaseNoticeSheetIOS(): ExternalPurchaseNoticeResultIOS {
        failWith(PurchaseError(code = ErrorCode.FeatureNotSupported, message = "External purchase notice sheet is iOS only"))
    }

    override suspend fun canPresentExternalPurchaseNoticeIOS(): Boolean {
        return false // Not supported on Android
    }

    override suspend fun userChoiceBillingAndroid(): UserChoiceBillingDetails {
        if (alternativeBillingMode != AlternativeBillingModeAndroid.UserChoice) {
            failWith(
                PurchaseError(
                    code = ErrorCode.DeveloperError,
                    message = "userChoiceBillingAndroid requires UserChoice alternative billing mode"
                )
            )
        }
        return _userChoiceBillingListener.first()
    }

    // ---------------------------------------------------------------------
    // Alternative Billing Methods (Android only)
    // ---------------------------------------------------------------------

    /**
     * Check if alternative billing is available for this user/device (Android only).
     * Step 1 of alternative billing flow.
     *
     * For AlternativeOnly mode: Uses isAlternativeBillingOnlyAvailableAsync
     * For UserChoice mode: Uses isFeatureSupported with ALTERNATIVE_BILLING feature
     */
    override suspend fun checkAlternativeBillingAvailabilityAndroid(): Boolean {
        return withContext(Dispatchers.IO) {
            val client = billingClient ?: run {
                failWith(PurchaseError(code = ErrorCode.NotPrepared, message = "Billing client not ready"))
            }

            when (alternativeBillingMode) {
                AlternativeBillingModeAndroid.AlternativeOnly -> {
                    suspendCancellableCoroutine { continuation ->
                        client.isAlternativeBillingOnlyAvailableAsync { billingResult ->
                            continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                        }
                    }
                }
                AlternativeBillingModeAndroid.UserChoice -> {
                    // User Choice Billing doesn't have a specific feature type constant
                    // It's enabled via enableUserChoiceBilling() and is available if alternative billing is supported
                    val result = client.isFeatureSupported(BillingClient.FeatureType.ALTERNATIVE_BILLING_ONLY)
                    result.responseCode == BillingClient.BillingResponseCode.OK
                }
                else -> false
            }
        }
    }

    /**
     * Show alternative billing information dialog to user (Android only).
     * Step 2 of alternative billing flow.
     * Must be called BEFORE processing payment in your payment system.
     *
     * Note: This is only applicable for AlternativeOnly mode.
     * For UserChoice mode, Google automatically shows selection dialog.
     */
    override suspend fun showAlternativeBillingDialogAndroid(): Boolean {
        // Only applicable for AlternativeOnly mode
        if (alternativeBillingMode != AlternativeBillingModeAndroid.AlternativeOnly) {
            failWith(PurchaseError(code = ErrorCode.DeveloperError, message = "showAlternativeBillingDialogAndroid is only for AlternativeOnly mode"))
        }

        return withContext(Dispatchers.Main) {
            val client = billingClient ?: run {
                failWith(PurchaseError(code = ErrorCode.NotPrepared, message = "Billing client not ready"))
            }
            val activity = currentActivity ?: run {
                failWith(PurchaseError(code = ErrorCode.ActivityUnavailable, message = "Activity not available"))
            }

            suspendCancellableCoroutine { continuation ->
                client.showAlternativeBillingOnlyInformationDialog(activity) { billingResult ->
                    val success = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    continuation.resume(success)
                }
            }
        }
    }

    /**
     * Create external transaction token for Google Play reporting (Android only).
     * Step 3 of alternative billing flow.
     * Must be called AFTER successful payment in your payment system.
     * Token must be reported to Google Play backend within 24 hours.
     *
     * Note: This is only applicable for AlternativeOnly mode.
     * For UserChoice mode, the token is provided in UserChoiceBillingDetails.
     */
    override suspend fun createAlternativeBillingTokenAndroid(): String? {
        // Only applicable for AlternativeOnly mode
        if (alternativeBillingMode != AlternativeBillingModeAndroid.AlternativeOnly) {
            failWith(PurchaseError(code = ErrorCode.DeveloperError, message = "createAlternativeBillingTokenAndroid is only for AlternativeOnly mode. For UserChoice mode, get token from UserChoiceBillingDetails"))
        }

        return withContext(Dispatchers.IO) {
            val client = billingClient ?: run {
                failWith(PurchaseError(code = ErrorCode.NotPrepared, message = "Billing client not ready"))
            }

            suspendCancellableCoroutine { continuation ->
                client.createAlternativeBillingOnlyReportingDetailsAsync { billingResult, alternativeBillingDetails ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(alternativeBillingDetails?.externalTransactionToken)
                    } else {
                        continuation.resume(null)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Billing Programs API (Android 8.2.0+)
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Activity lifecycle
    // ---------------------------------------------------------------------
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (currentActivity == null) currentActivity = activity
    }
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }
}
