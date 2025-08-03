package dev.hyo.martie.navigation

import androidx.compose.runtime.*
import dev.hyo.martie.screens.*

@Composable
fun Navigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    
    when (currentScreen) {
        Screen.Home -> HomeScreen(
            onNavigate = { currentScreen = it }
        )
        Screen.AvailablePurchases -> AvailablePurchasesScreen(
            onBack = { currentScreen = Screen.Home }
        )
        Screen.PurchaseFlow -> PurchaseFlowScreen(
            onBack = { currentScreen = Screen.Home }
        )
        Screen.SubscriptionFlow -> SubscriptionFlowScreen(
            onBack = { currentScreen = Screen.Home }
        )
        Screen.OfferCode -> OfferCodeScreen(
            onBack = { currentScreen = Screen.Home }
        )
    }
}

sealed interface Screen {
    data object Home : Screen
    data object AvailablePurchases : Screen
    data object PurchaseFlow : Screen
    data object SubscriptionFlow : Screen
    data object OfferCode : Screen
}