package com.example.exchangededivisas.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.exchangededivisas.presentation.admin.AdminDashboardScreen
import com.example.exchangededivisas.presentation.auth.LoginScreen
import com.example.exchangededivisas.presentation.auth.RegisterScreen
import com.example.exchangededivisas.presentation.currencies.CurrenciesScreen
import com.example.exchangededivisas.presentation.history.HistoryScreen
import com.example.exchangededivisas.presentation.home.HomeScreen
import com.example.exchangededivisas.presentation.settings.SettingsScreen
import com.example.exchangededivisas.presentation.transactions.TransactionsScreen
import com.example.exchangededivisas.presentation.wallet.WalletScreen
import com.example.exchangededivisas.presentation.welcome.WelcomeScreen
import com.example.exchangededivisas.presentation.deposit.DepositScreen
import com.example.exchangededivisas.presentation.pairs.PairDetailScreen
import com.example.exchangededivisas.presentation.pairs.PairsScreen
import com.example.exchangededivisas.presentation.withdraw.WithdrawScreen
import com.example.exchangededivisas.presentation.trade.InstantBuyScreen
import com.example.exchangededivisas.presentation.trade.InstantSaleScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        // --- Pantallas SIN barra de navegación ---
        composable("welcome") { WelcomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }

        // --- Panel Administrativo (Tiene su propia barra lateral, va independiente) ---
        composable("admin_dashboard") {
            // Criterio de aceptación: Validación de rol restringido a ADM
            AdminDashboardScreen(navController = navController, userRole = "ADM")
        }

        // --- Pantallas CON barra de navegación (las 7 opciones del usuario común) ---
        composable("home") { MainScaffold(navController) { HomeScreen() } }
        composable("wallet") { MainScaffold(navController) { WalletScreen(navController) } }
        composable("currencies") { MainScaffold(navController) { PairsScreen(navController) } }
        composable("transactions") { MainScaffold(navController) { TransactionsScreen() } }
        composable("history") { MainScaffold(navController) { HistoryScreen() } }
        composable("settings") { MainScaffold(navController) { SettingsScreen() } }
        composable("deposit") { MainScaffold(navController) { DepositScreen() } }
        composable("withdraw") { MainScaffold(navController) { WithdrawScreen(navController) } }
        composable("instantBuy") { MainScaffold(navController) { InstantBuyScreen() } }
        composable("ventaInmediata/{code}") { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code") ?: "USD/PEN"
            InstantSaleScreen(navController, code)
        }
        composable("pairs") { MainScaffold(navController) { PairsScreen(navController) } }
        composable("pairDetail/{code}") { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code") ?: ""
            MainScaffold(navController) { PairDetailScreen(navController, code) }
        }

    }
}