package com.example.exchangededivisas.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

// Las 7 opciones del prototipo
private val navItems = listOf(
    NavItem("home", "Menú", Icons.Default.Home),
    NavItem("wallet", "Billetera", Icons.Default.AccountBalanceWallet),
    NavItem("currencies", "Monedas", Icons.Default.CurrencyExchange),
    NavItem("transactions", "Transacciones", Icons.Default.Description),
    NavItem("history", "Historial", Icons.Default.History),
    NavItem("settings", "Configuración", Icons.Default.Settings),
    NavItem("logout", "Salir", Icons.AutoMirrored.Filled.ExitToApp)
)

@Composable
fun MainScaffold(
    navController: NavController,
    content: @Composable () -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Barra en filas de 4 (porque son 7 opciones)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 8.dp)
            ) {
                // Dividimos las 7 en filas de 4
                navItems.chunked(4).forEach { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        fila.forEach { item ->
                            NavBoton(
                                item = item,
                                seleccionado = currentRoute == item.route,
                                onClick = {
                                    if (item.route == "logout") {
                                        // "Salir" vuelve al login
                                        navController.navigate("login") {
                                            popUpTo(0)
                                        }
                                    } else if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}

@Composable
private fun NavBoton(item: NavItem, seleccionado: Boolean, onClick: () -> Unit) {
    val color = if (seleccionado) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(item.icon, contentDescription = item.label, tint = color)
        Text(item.label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
