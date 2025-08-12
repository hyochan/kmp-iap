import StoreKit
import Foundation

// MARK: - Data Models for StoreKit 2

@available(iOS 15.0, *)
@objc public class KmpProduct: NSObject {
    @objc public let productId: String
    @objc public let displayName: String
    @objc public let description: String
    @objc public let displayPrice: String
    @objc public let price: Decimal
    @objc public let priceFormatStyle: String
    @objc public let type: String
    @objc public let isFamilyShareable: Bool
    @objc public let subscription: KmpSubscriptionInfo?
    @objc public let jsonRepresentation: String
    
    init(from product: Product) {
        self.productId = product.id
        self.displayName = product.displayName
        self.description = product.description
        self.displayPrice = product.displayPrice
        self.price = product.price
        self.priceFormatStyle = product.priceFormatStyle.formatted()
        
        switch product.type {
        case .consumable:
            self.type = "consumable"
        case .nonConsumable:
            self.type = "nonConsumable"
        case .autoRenewable:
            self.type = "autoRenewable"
        case .nonRenewable:
            self.type = "nonRenewable"
        default:
            self.type = "unknown"
        }
        
        self.isFamilyShareable = product.isFamilyShareable
        
        if let subscription = product.subscription {
            self.subscription = KmpSubscriptionInfo(from: subscription, product: product)
        } else {
            self.subscription = nil
        }
        
        self.jsonRepresentation = product.jsonRepresentation
    }
}

@available(iOS 15.0, *)
@objc public class KmpSubscriptionInfo: NSObject {
    @objc public let subscriptionGroupID: String
    @objc public let subscriptionPeriod: KmpSubscriptionPeriod
    @objc public let introductoryOffer: KmpSubscriptionOffer?
    @objc public let promotionalOffers: [KmpSubscriptionOffer]
    @objc public let isEligibleForIntroOffer: Bool
    
    init(from subscription: Product.SubscriptionInfo, product: Product) {
        self.subscriptionGroupID = subscription.subscriptionGroupID
        self.subscriptionPeriod = KmpSubscriptionPeriod(from: subscription.subscriptionPeriod)
        
        if let intro = subscription.introductoryOffer {
            self.introductoryOffer = KmpSubscriptionOffer(from: intro)
        } else {
            self.introductoryOffer = nil
        }
        
        self.promotionalOffers = subscription.promotionalOffers.map { KmpSubscriptionOffer(from: $0) }
        
        // Check eligibility for intro offer
        self.isEligibleForIntroOffer = subscription.isEligibleForIntroOffer
    }
}

@available(iOS 15.0, *)
@objc public class KmpSubscriptionPeriod: NSObject {
    @objc public let value: Int
    @objc public let unit: String
    
    init(from period: Product.SubscriptionPeriod) {
        self.value = period.value
        
        switch period.unit {
        case .day:
            self.unit = "day"
        case .week:
            self.unit = "week"
        case .month:
            self.unit = "month"
        case .year:
            self.unit = "year"
        @unknown default:
            self.unit = "unknown"
        }
    }
}

@available(iOS 15.0, *)
@objc public class KmpSubscriptionOffer: NSObject {
    @objc public let id: String?
    @objc public let type: String
    @objc public let displayPrice: String
    @objc public let period: KmpSubscriptionPeriod
    @objc public let periodCount: Int
    @objc public let paymentMode: String
    
    init(from offer: Product.SubscriptionOffer) {
        self.id = offer.id
        
        switch offer.type {
        case .introductory:
            self.type = "introductory"
        case .promotional:
            self.type = "promotional"
        default:
            self.type = "unknown"
        }
        
        self.displayPrice = offer.displayPrice
        self.period = KmpSubscriptionPeriod(from: offer.period)
        self.periodCount = offer.periodCount
        
        switch offer.paymentMode {
        case .freeTrial:
            self.paymentMode = "freeTrial"
        case .payAsYouGo:
            self.paymentMode = "payAsYouGo"
        case .payUpFront:
            self.paymentMode = "payUpFront"
        default:
            self.paymentMode = "unknown"
        }
    }
}

@available(iOS 15.0, *)
@objc public class KmpPurchase: NSObject {
    @objc public let id: UInt64
    @objc public let productId: String
    @objc public let purchaseDate: Date
    @objc public let originalPurchaseDate: Date
    @objc public let expirationDate: Date?
    @objc public let quantity: Int
    @objc public let productType: String
    @objc public let appAccountToken: UUID?
    @objc public let originalId: UInt64
    @objc public let isUpgraded: Bool
    @objc public let offerType: String?
    @objc public let offerID: String?
    @objc public let revocationDate: Date?
    @objc public let revocationReason: String?
    @objc public let jsonRepresentation: String
    @objc public let jwsRepresentation: String
    @objc public let environment: String
    
    init(from transaction: Transaction) {
        self.id = transaction.id
        self.productId = transaction.productID
        self.purchaseDate = transaction.purchaseDate
        self.originalPurchaseDate = transaction.originalPurchaseDate
        self.expirationDate = transaction.expirationDate
        self.quantity = transaction.purchasedQuantity
        
        switch transaction.productType {
        case .consumable:
            self.productType = "consumable"
        case .nonConsumable:
            self.productType = "nonConsumable"
        case .autoRenewable:
            self.productType = "autoRenewable"
        case .nonRenewable:
            self.productType = "nonRenewable"
        default:
            self.productType = "unknown"
        }
        
        self.appAccountToken = transaction.appAccountToken
        self.originalId = transaction.originalID
        self.isUpgraded = transaction.isUpgraded
        
        if let offerType = transaction.offerType {
            switch offerType {
            case .introductory:
                self.offerType = "introductory"
            case .promotional:
                self.offerType = "promotional"
            case .code:
                self.offerType = "code"
            default:
                self.offerType = "unknown"
            }
        } else {
            self.offerType = nil
        }
        
        self.offerID = transaction.offerID
        self.revocationDate = transaction.revocationDate
        
        if let reason = transaction.revocationReason {
            switch reason {
            case .developerIssue:
                self.revocationReason = "developerIssue"
            case .other:
                self.revocationReason = "other"
            default:
                self.revocationReason = "unknown"
            }
        } else {
            self.revocationReason = nil
        }
        
        self.jsonRepresentation = transaction.jsonRepresentation
        self.jwsRepresentation = transaction.jwsRepresentation
        
        switch transaction.environment {
        case .production:
            self.environment = "production"
        case .sandbox:
            self.environment = "sandbox"
        case .xcode:
            self.environment = "xcode"
        default:
            self.environment = "unknown"
        }
    }
}

@objc public class KmpPurchaseError: NSObject {
    @objc public let message: String
    @objc public let code: String
    @objc public let productId: String?
    
    init(message: String, code: String, productId: String?) {
        self.message = message
        self.code = code
        self.productId = productId
    }
}

@objc public class ConnectionResult: NSObject {
    @objc public let connected: Bool
    @objc public let message: String
    
    init(connected: Bool, message: String) {
        self.connected = connected
        self.message = message
    }
}

@objc public class AppStoreInfo: NSObject {
    @objc public let countryCode: String
    @objc public let identifier: String
    
    init(countryCode: String, identifier: String) {
        self.countryCode = countryCode
        self.identifier = identifier
    }
}

// MARK: - Receipt Validation Response

@objc public class ReceiptValidationResponse: NSObject {
    @objc public let status: Int
    @objc public let receipt: [String: Any]?
    @objc public let latestReceiptInfo: [[String: Any]]?
    @objc public let pendingRenewalInfo: [[String: Any]]?
    @objc public let isRetryable: Bool
    
    init(from json: [String: Any]) {
        self.status = json["status"] as? Int ?? -1
        self.receipt = json["receipt"] as? [String: Any]
        self.latestReceiptInfo = json["latest_receipt_info"] as? [[String: Any]]
        self.pendingRenewalInfo = json["pending_renewal_info"] as? [[String: Any]]
        self.isRetryable = json["is-retryable"] as? Bool ?? false
    }
}

// MARK: - Transaction Info for StoreKit 2

@available(iOS 15.0, *)
@objc public class KmpTransactionInfo: NSObject {
    @objc public let appAccountToken: UUID?
    @objc public let bundleID: String
    @objc public let currency: String?
    @objc public let deviceVerification: String
    @objc public let deviceVerificationNonce: UUID
    @objc public let environment: String
    @objc public let expirationDate: Date?
    @objc public let isUpgraded: Bool
    @objc public let offerID: String?
    @objc public let offerType: String?
    @objc public let originalID: UInt64
    @objc public let originalPurchaseDate: Date
    @objc public let price: Decimal?
    @objc public let productID: String
    @objc public let productType: String
    @objc public let purchaseDate: Date
    @objc public let purchasedQuantity: Int
    @objc public let revocationDate: Date?
    @objc public let revocationReason: String?
    @objc public let signedDate: Date
    @objc public let storefront: String
    @objc public let storefrontID: String
    @objc public let subscriptionGroupID: String?
    @objc public let transactionID: UInt64
    @objc public let webOrderLineItemID: String?
    
    init(from transaction: Transaction) {
        self.appAccountToken = transaction.appAccountToken
        self.bundleID = transaction.bundleID
        self.currency = transaction.currency
        self.deviceVerification = transaction.deviceVerification
        self.deviceVerificationNonce = transaction.deviceVerificationNonce
        
        switch transaction.environment {
        case .production:
            self.environment = "production"
        case .sandbox:
            self.environment = "sandbox"
        case .xcode:
            self.environment = "xcode"
        default:
            self.environment = "unknown"
        }
        
        self.expirationDate = transaction.expirationDate
        self.isUpgraded = transaction.isUpgraded
        self.offerID = transaction.offerID
        
        if let offerType = transaction.offerType {
            switch offerType {
            case .introductory:
                self.offerType = "introductory"
            case .promotional:
                self.offerType = "promotional"
            case .code:
                self.offerType = "code"
            default:
                self.offerType = "unknown"
            }
        } else {
            self.offerType = nil
        }
        
        self.originalID = transaction.originalID
        self.originalPurchaseDate = transaction.originalPurchaseDate
        self.price = transaction.price
        self.productID = transaction.productID
        
        switch transaction.productType {
        case .consumable:
            self.productType = "consumable"
        case .nonConsumable:
            self.productType = "nonConsumable"
        case .autoRenewable:
            self.productType = "autoRenewable"
        case .nonRenewable:
            self.productType = "nonRenewable"
        default:
            self.productType = "unknown"
        }
        
        self.purchaseDate = transaction.purchaseDate
        self.purchasedQuantity = transaction.purchasedQuantity
        self.revocationDate = transaction.revocationDate
        
        if let reason = transaction.revocationReason {
            switch reason {
            case .developerIssue:
                self.revocationReason = "developerIssue"
            case .other:
                self.revocationReason = "other"
            default:
                self.revocationReason = "unknown"
            }
        } else {
            self.revocationReason = nil
        }
        
        self.signedDate = transaction.signedDate
        self.storefront = transaction.storefront
        self.storefrontID = transaction.storefrontID
        self.subscriptionGroupID = transaction.subscriptionGroupID
        self.transactionID = transaction.id
        self.webOrderLineItemID = transaction.webOrderLineItemID
    }
}