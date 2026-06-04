package com.example.exchangededivisas.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.exchangededivisas.presentation.auth.LoginScreen
import com.example.exchangededivisas.presentation.auth.RegisterScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "register"   // arranca DIRECTO en tu registro
    ) {
        composable("register") { RegisterScreen(navController) }
        composable("login") { LoginScreen(navController) }
    }
}