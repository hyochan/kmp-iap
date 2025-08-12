import StoreKit
import Foundation

@available(iOS 15.0, *)
@objc public class KmpIAPManager: NSObject {
    @objc public static let shared = KmpIAPManager()
    
    private var updateListenerTask: Task<Void, Error>?
    private var productCache: [String: Product] = [:]
    private var isConnected = false
    
    // Callbacks for Kotlin
    @objc public var onPurchaseUpdated: ((KmpPurchase) -> Void)?
    @objc public var onPurchaseError: ((KmpPurchaseError) -> Void)?
    @objc public var onConnectionStateChanged: ((ConnectionResult) -> Void)?
    @objc public var onPromotedProduct: ((String) -> Void)?
    
    private override init() {
        super.init()
    }
    
    @objc public func initConnection() {
        guard updateListenerTask == nil else { return }
        
        updateListenerTask = listenForTransactions()
        isConnected = true
        
        onConnectionStateChanged?(ConnectionResult(
            connected: true,
            message: "Connected to App Store"
        ))
        
        // Check for unfinished transactions
        Task {
            await checkUnfinishedTransactions()
        }
    }
    
    @objc public func endConnection() {
        updateListenerTask?.cancel()
        updateListenerTask = nil
        isConnected = false
        productCache.removeAll()
        
        onConnectionStateChanged?(ConnectionResult(
            connected: false,
            message: "Disconnected from App Store"
        ))
    }
    
    @objc public func requestProducts(
        productIds: [String],
        completion: @escaping ([KmpProduct]?, Error?) -> Void
    ) {
        guard isConnected else {
            completion(nil, NSError(
                domain: "KmpIAP",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Not connected to App Store"]
            ))
            return
        }
        
        Task {
            do {
                let products = try await Product.products(for: Set(productIds))
                
                // Cache products
                for product in products {
                    productCache[product.id] = product
                }
                
                // Convert to KMP Product type
                let kmpProducts = products.compactMap { product in
                    KmpProduct(from: product)
                }
                
                completion(kmpProducts, nil)
            } catch {
                completion(nil, error)
            }
        }
    }
    
    @objc public func requestPurchase(
        productId: String,
        quantity: Int = 1,
        appAccountToken: UUID? = nil,
        completion: @escaping (Bool) -> Void
    ) {
        guard isConnected else {
            onPurchaseError?(KmpPurchaseError(
                message: "Not connected to App Store",
                code: "E_NOT_INITIALIZED",
                productId: productId
            ))
            completion(false)
            return
        }
        
        Task {
            do {
                // Get product from cache or fetch it
                let product: Product
                if let cached = productCache[productId] {
                    product = cached
                } else {
                    let products = try await Product.products(for: [productId])
                    guard let fetchedProduct = products.first else {
                        onPurchaseError?(KmpPurchaseError(
                            message: "Product not found",
                            code: "E_ITEM_UNAVAILABLE",
                            productId: productId
                        ))
                        completion(false)
                        return
                    }
                    product = fetchedProduct
                    productCache[productId] = product
                }
                
                // Create purchase options
                var options: Set<Product.PurchaseOption> = []
                if let token = appAccountToken {
                    options.insert(.appAccountToken(token))
                }
                if quantity > 1 {
                    options.insert(.quantity(quantity))
                }
                
                // Attempt purchase
                let result = try await product.purchase(options: options)
                
                switch result {
                case .success(let verification):
                    switch verification {
                    case .verified(let transaction):
                        // Finish the transaction
                        await transaction.finish()
                        
                        // Notify success
                        let purchase = KmpPurchase(from: transaction)
                        onPurchaseUpdated?(purchase)
                        completion(true)
                        
                    case .unverified(let transaction, let error):
                        // Handle unverified transaction
                        await transaction.finish()
                        
                        onPurchaseError?(KmpPurchaseError(
                            message: "Transaction verification failed: \(error)",
                            code: "E_VERIFICATION_FAILED",
                            productId: productId
                        ))
                        completion(false)
                    }
                    
                case .userCancelled:
                    onPurchaseError?(KmpPurchaseError(
                        message: "User cancelled the purchase",
                        code: "E_USER_CANCELLED",
                        productId: productId
                    ))
                    completion(false)
                    
                case .pending:
                    onPurchaseError?(KmpPurchaseError(
                        message: "Purchase is pending approval",
                        code: "E_DEFERRED_PAYMENT",
                        productId: productId
                    ))
                    completion(false)
                    
                @unknown default:
                    completion(false)
                }
            } catch {
                onPurchaseError?(KmpPurchaseError(
                    message: error.localizedDescription,
                    code: "E_PURCHASE_FAILED",
                    productId: productId
                ))
                completion(false)
            }
        }
    }
    
    @objc public func restorePurchases(completion: @escaping ([KmpPurchase]) -> Void) {
        guard isConnected else {
            completion([])
            return
        }
        
        Task {
            do {
                // Sync with App Store
                try await AppStore.sync()
                
                // Get all current entitlements
                var restoredPurchases: [KmpPurchase] = []
                
                for await result in Transaction.currentEntitlements {
                    switch result {
                    case .verified(let transaction):
                        restoredPurchases.append(KmpPurchase(from: transaction))
                    case .unverified(_, _):
                        // Skip unverified transactions
                        continue
                    }
                }
                
                completion(restoredPurchases)
            } catch {
                onPurchaseError?(KmpPurchaseError(
                    message: error.localizedDescription,
                    code: "E_RESTORE_FAILED",
                    productId: nil
                ))
                completion([])
            }
        }
    }
    
    @objc public func getAvailablePurchases(completion: @escaping ([KmpPurchase]) -> Void) {
        guard isConnected else {
            completion([])
            return
        }
        
        Task {
            var purchases: [KmpPurchase] = []
            
            // Get all current entitlements (active subscriptions and non-consumables)
            for await result in Transaction.currentEntitlements {
                switch result {
                case .verified(let transaction):
                    purchases.append(KmpPurchase(from: transaction))
                case .unverified(_, _):
                    continue
                }
            }
            
            completion(purchases)
        }
    }
    
    @objc public func finishTransaction(transactionId: UInt64) async {
        // In StoreKit 2, transactions are finished automatically
        // This method is kept for compatibility
        for await result in Transaction.updates {
            switch result {
            case .verified(let transaction):
                if transaction.id == transactionId {
                    await transaction.finish()
                    return
                }
            case .unverified(_, _):
                continue
            }
        }
    }
    
    @objc public func canMakePayments() -> Bool {
        return AppStore.canMakePayments
    }
    
    @objc public func getStorefront() async -> AppStoreInfo? {
        guard let storefront = await Storefront.current else { return nil }
        
        return AppStoreInfo(
            countryCode: storefront.countryCode,
            identifier: storefront.id
        )
    }
    
    @objc public func presentOfferCodeRedemptionSheet() {
        Task {
            do {
                try await AppStore.presentOfferCodeRedeemSheet()
            } catch {
                onPurchaseError?(KmpPurchaseError(
                    message: "Failed to present offer code redemption sheet",
                    code: "E_REDEMPTION_FAILED",
                    productId: nil
                ))
            }
        }
    }
    
    @objc public func showManageSubscriptions() {
        Task {
            do {
                try await AppStore.showManageSubscriptions()
            } catch {
                // Fallback to opening App Store subscriptions page
                if let url = URL(string: "https://apps.apple.com/account/subscriptions"),
                   await UIApplication.shared.canOpenURL(url) {
                    await UIApplication.shared.open(url, options: [:])
                }
            }
        }
    }
    
    @objc public func validateReceipt(
        receiptData: String,
        isTest: Bool,
        completion: @escaping ([String: Any]?, Error?) -> Void
    ) {
        // StoreKit 2 uses JWS for verification instead of receipts
        // For backward compatibility, we can still validate using the old endpoint
        Task {
            let url = isTest
                ? "https://sandbox.itunes.apple.com/verifyReceipt"
                : "https://buy.itunes.apple.com/verifyReceipt"
            
            guard let requestUrl = URL(string: url) else {
                completion(nil, NSError(
                    domain: "KmpIAP",
                    code: -1,
                    userInfo: [NSLocalizedDescriptionKey: "Invalid URL"]
                ))
                return
            }
            
            var request = URLRequest(url: requestUrl)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            
            let body = ["receipt-data": receiptData]
            request.httpBody = try? JSONSerialization.data(withJSONObject: body)
            
            do {
                let (data, _) = try await URLSession.shared.data(for: request)
                let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
                completion(json, nil)
            } catch {
                completion(nil, error)
            }
        }
    }
    
    // MARK: - Private Methods
    
    private func listenForTransactions() -> Task<Void, Error> {
        return Task.detached {
            // Listen for transaction updates
            for await result in Transaction.updates {
                await self.handle(transactionResult: result)
            }
        }
    }
    
    private func handle(transactionResult: VerificationResult<Transaction>) async {
        switch transactionResult {
        case .verified(let transaction):
            // Handle verified transaction
            let purchase = KmpPurchase(from: transaction)
            
            // Deliver content
            onPurchaseUpdated?(purchase)
            
            // Finish transaction
            await transaction.finish()
            
        case .unverified(let transaction, let error):
            // Handle unverified transaction
            onPurchaseError?(KmpPurchaseError(
                message: "Transaction verification failed: \(error)",
                code: "E_VERIFICATION_FAILED",
                productId: transaction.productID
            ))
            
            // Still finish the transaction to avoid blocking
            await transaction.finish()
        }
    }
    
    private func checkUnfinishedTransactions() async {
        // Check for unfinished transactions
        for await result in Transaction.unfinished {
            switch result {
            case .verified(let transaction):
                // Handle the transaction
                let purchase = KmpPurchase(from: transaction)
                onPurchaseUpdated?(purchase)
                await transaction.finish()
                
            case .unverified(let transaction, _):
                // Finish unverified transactions without processing
                await transaction.finish()
            }
        }
    }
}

// MARK: - Fallback for iOS < 15.0

@objc public class KmpIAPManagerLegacy: NSObject {
    // This would contain the StoreKit 1 implementation
    // Similar to the original implementation but wrapped for Obj-C
}