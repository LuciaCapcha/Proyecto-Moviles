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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.exchangededivisas.data.session.AppSession
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.repository.WalletCurrencyUi

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem("home", "Menú", Icons.Default.Home),
    NavItem("wallet", "Billetera", Icons.Default.AccountBalanceWallet),
    NavItem("currencies", "Monedas", Icons.Default.CurrencyExchange),
    NavItem("transactions", "Transacciones", Icons.Default.Description),
    NavItem("history", "Historial", Icons.Default.History),
    NavItem("settings", "Config", Icons.Default.Settings),
    NavItem("logout", "Salir", Icons.AutoMirrored.Filled.ExitToApp)
)

@Composable
fun MainScaffold(
    navController: NavController,
    content: @Composable () -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentUser by AppSession.currentUser.collectAsState()

    // Carga de balances
    var balances by remember { mutableStateOf<List<WalletCurrencyUi>>(emptyList()) }
    val walletTick by AppSession.walletRefreshTick.collectAsState()

    LaunchedEffect(currentUser.usuarioId, walletTick) {
        runCatching {
            ExchangeRepository.loadWallet(currentUser.usuarioId)
        }.onSuccess { list ->
            balances = list.filter { it.balance > 0.0 }
        }.onFailure {
            balances = emptyList()
        }
    }

    Scaffold(
        topBar = {
            WalletTopBar(balances = balances)
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 8.dp)
            ) {
                // Saludo con usuario
                Text(
                    text = "Hola, ${currentUser.nombreUsuario}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                )
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
                                        AppSession.logout()
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
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}

@Composable
private fun WalletTopBar(balances: List<WalletCurrencyUi>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Tus Activos",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (balances.isEmpty()) {
                Text("Sin saldo", fontSize = 12.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(balances) { currency ->
                        Column {
                            Text(
                                text = currency.code,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "%.2f".format(currency.balance),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavBoton(item: NavItem, seleccionado: Boolean, onClick: () -> Unit) {
    val color = if (seleccionado) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = color
        )
        Text(
            text = item.label,
            fontSize = 10.sp,
            color = color,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal
        )
    }
}
