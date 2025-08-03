package dev.hyo.martie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.hyo.martie.utils.swipeToBack
import dev.hyo.martie.theme.AppColors
import io.github.hyochan.kmpiap.types.Purchase
import io.github.hyochan.kmpiap.useIap.useIap
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailablePurchasesScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val iapHelper = remember { useIap(scope = scope) }
    
    var isLoading by remember { mutableStateOf(true) }  // Start with true
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val connected by iapHelper.connected.collectAsState()
    val availablePurchases by iapHelper.availablePurchases.collectAsState()
    
    // Initialize connection and load purchases automatically
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            iapHelper.initConnection()
        } catch (e: Exception) {
            error = "Failed to initialize: ${e.message}"
            isLoading = false
        }
    }
    
    // Load purchases when connected
    LaunchedEffect(connected) {
        if (connected) {
            // Add delay to ensure connection is fully established
            kotlinx.coroutines.delay(500)
            try {
                println("[AvailablePurchasesScreen] Calling getAvailablePurchases...")
                iapHelper.getAvailablePurchases()
                println("[AvailablePurchasesScreen] getAvailablePurchases completed")
            } catch (e: Exception) {
                error = "Failed to load purchases: ${e.message}"
                println("[AvailablePurchasesScreen] Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            iapHelper.dispose()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Purchases") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AppColors.OnSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColors.Background)
                .swipeToBack(navController)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Purchases",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )
                
                // Refresh Button
                IconButton(
                    onClick = {
                        scope.launch {
                            isRefreshing = true
                            error = null
                            try {
                                println("[AvailablePurchasesScreen] Refreshing purchases...")
                                iapHelper.getAvailablePurchases()
                                println("[AvailablePurchasesScreen] Refresh completed")
                            } catch (e: Exception) {
                                error = "Failed to refresh purchases: ${e.message}"
                                println("[AvailablePurchasesScreen] Refresh error: ${e.message}")
                            } finally {
                                isRefreshing = false
                            }
                        }
                    },
                    enabled = !isRefreshing && !isLoading
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AppColors.Primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AppColors.Primary
                        )
                    }
                }
            }
            
            // Error Display
            error?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Error)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        color = Color.White
                    )
                }
            }
            
            // Purchases List or Loading/Empty State
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = AppColors.Primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading purchases...",
                                color = AppColors.Secondary,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                availablePurchases.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No purchases found",
                                fontSize = 16.sp,
                                color = AppColors.Secondary
                            )
                        }
                    }
                }
                else -> {
                    availablePurchases.forEach { purchase ->
                        PurchaseCard(purchase)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            // Info Section
            Spacer(modifier = Modifier.height(20.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ℹ️ Available Purchases",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = AppColors.OnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Shows purchased items that haven't been consumed\n" +
                              "• Useful for restoring purchases on new devices\n" +
                              "• Automatically refreshes when you open this screen",
                        fontSize = 14.sp,
                        color = AppColors.Secondary
                    )
                }
            }
        }
    }
}

@Composable
fun PurchaseCard(purchase: Purchase) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = purchase.productId,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = AppColors.OnSurface
            )
            
            purchase.transactionId?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Transaction: $it",
                    fontSize = 12.sp,
                    color = AppColors.Secondary,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            purchase.transactionDate?.let { date ->
                Spacer(modifier = Modifier.height(4.dp))
                val localDate = date.toLocalDateTime(TimeZone.currentSystemDefault())
                Text(
                    text = "Date: ${localDate.date} ${localDate.time}",
                    fontSize = 12.sp,
                    color = AppColors.Secondary
                )
            }
            
            purchase.transactionState?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "State: $it",
                    fontSize = 12.sp,
                    color = AppColors.Secondary
                )
            }
        }
    }
}