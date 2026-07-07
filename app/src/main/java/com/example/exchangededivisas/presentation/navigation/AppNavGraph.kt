package com.example.exchangededivisas.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.exchangededivisas.presentation.admin.AdminDashboardScreen
import com.example.exchangededivisas.presentation.auth.LoginScreen
import com.example.exchangededivisas.presentation.auth.RegisterScreen
import com.example.exchangededivisas.presentation.deposit.DepositScreen
import com.example.exchangededivisas.presentation.history.HistoryScreen
import com.example.exchangededivisas.presentation.home.HomeScreen
import com.example.exchangededivisas.presentation.pairs.PairDetailScreen
import com.example.exchangededivisas.presentation.pairs.PairsScreen
import com.example.exchangededivisas.presentation.settings.SettingsScreen
import com.example.exchangededivisas.presentation.trade.InstantBuyScreen
import com.example.exchangededivisas.presentation.trade.InstantSaleScreen
import com.example.exchangededivisas.presentation.transactions.TransactionsScreen
import com.example.exchangededivisas.presentation.wallet.WalletScreen
import com.example.exchangededivisas.presentation.welcome.WelcomeScreen
import com.example.exchangededivisas.presentation.withdraw.WithdrawScreen
import com.example.exchangededivisas.presentation.orderbook.OrderBookScreen
import com.example.exchangededivisas.presentation.trade.CreateOrderScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") { WelcomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }

        composable("admin_dashboard") {
            MainScaffold(navController) {
                AdminDashboardScreen(
                    navController = navController,
                    userRole = "ADM",
                    initialScreen = "Visión general de operaciones"
                )
            }
        }

        composable("admin_users") {
            MainScaffold(navController) {
                AdminDashboardScreen(
                    navController = navController,
                    userRole = "ADM",
                    initialScreen = "Gestión de usuarios"
                )
            }
        }

        // --- Pantallas CON barra de navegación ---
        composable("home") { 
            MainScaffold(navController) { 
                HomeScreen(
                    navController = navController,
                    onNavigateToLogin = { navController.navigate("login") },
                    onNavigateToRegister = { navController.navigate("register") }
                ) 
            } 
        }
        composable("wallet") { MainScaffold(navController) { WalletScreen(navController) } }
        composable("currencies") { MainScaffold(navController) { PairsScreen(navController) } }
        composable("transactions") { MainScaffold(navController) { TransactionsScreen() } }
        composable("history") { MainScaffold(navController) { HistoryScreen() } }
        composable("settings") { MainScaffold(navController) { SettingsScreen() } }
        composable("deposit") { MainScaffold(navController) { DepositScreen() } }
        composable("withdraw") { MainScaffold(navController) { WithdrawScreen(navController) } }
        
        composable("instantBuy/{code}") { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code") ?: "USD_PEN"
            MainScaffold(navController) { InstantBuyScreen(navController, code) }
        }
        
        // Mantener la ruta sin parámetros por compatibilidad si se usa en otros lados
        composable("instantBuy") {
            MainScaffold(navController) { InstantBuyScreen(navController) }
        }
        
        composable("ventaInmediata/{code}") { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code") ?: "USD_PEN"
            MainScaffold(navController) { InstantSaleScreen(navController, code) }
        }

        composable("pairDetail/{code}") { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code") ?: "PEN_USD"
            MainScaffold(navController) { PairDetailScreen(navController, code) }
        }

        composable("orderbook") {
            MainScaffold(navController) { OrderBookScreen(currencyPair = "USD/PEN") }
        }

        composable("orderbook/{currencyPair}") { backStackEntry ->
            val pair = backStackEntry.arguments?.getString("currencyPair") ?: "USD_PEN"
            MainScaffold(navController) {
                OrderBookScreen(currencyPair = pair.replace("_", "/"))
            }
        }
        
        composable("createOrder/{parId}") { backStackEntry ->
            val parId = backStackEntry.arguments?.getString("parId")?.toIntOrNull() ?: 1
            MainScaffold(navController) { CreateOrderScreen(parMonedaId = parId) }
        }
    }
}
