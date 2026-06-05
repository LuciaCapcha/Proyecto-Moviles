package com.example.exchangededivisas.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.exchangededivisas.presentation.auth.LoginScreen
import com.example.exchangededivisas.presentation.auth.RegisterScreen
import com.example.exchangededivisas.presentation.home.HomeScreen
import com.example.exchangededivisas.presentation.welcome.WelcomeScreen

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
        composable("home") { HomeScreen() }
    }
}