package com.example.exchangededivisas.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.exchangededivisas.data.repository.ActiveTradeType
import com.example.exchangededivisas.data.repository.ActiveTradeUi
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.launch

@Composable
fun TransactionsScreen() {
    val scope = rememberCoroutineScope()
    val currentUser by AppSession.currentUser.collectAsState()
    val refreshTick by AppSession.walletRefreshTick.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<ActiveTradeUi>>(emptyList()) }
    var selectedToCancel by remember { mutableStateOf<ActiveTradeUi?>(null) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            error = null

            ExchangeRepository.loadActiveTransactions(currentUser.usuarioId)
                .onSuccess { items = it }
                .onFailure { error = it.message ?: "No se pudieron cargar las transacciones." }

            isLoading = false
        }
    }

    LaunchedEffect(currentUser.usuarioId, refreshTick) {
        load()
    }

    val buyOrders = items.filter { it.type == ActiveTradeType.BUY_ORDER }
    val sellOffers = items.filter { it.type == ActiveTradeType.SELL_OFFER }
    val visibleItems = if (selectedTab == 0) buyOrders else sellOffers

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Transacciones activas",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Órdenes y ofertas activas cargadas desde Supabase.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Órdenes de compra (${buyOrders.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Ofertas de venta (${sellOffers.size})") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando transacciones...")
            return@Column
        }

        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { load() }) {
                Text("Reintentar")
            }
            return@Column
        }

        if (visibleItems.isEmpty()) {
            Text("No hay registros activos.")
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visibleItems, key = { "${it.type}-${it.id}" }) { item ->
                ActiveTransactionCard(
                    item = item,
                    onCancel = { selectedToCancel = item }
                )
            }
        }
    }

    if (selectedToCancel != null) {
        val item = selectedToCancel!!

        AlertDialog(
            onDismissRequest = { selectedToCancel = null },
            title = { Text("Cancelar operación") },
            text = {
                Text(
                    "¿Deseas cancelar esta operación? Se reembolsarán %.2f %s."
                        .format(item.refundAmount, item.refundCurrency)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            ExchangeRepository.cancelActiveTransaction(
                                usuarioId = currentUser.usuarioId,
                                item = item
                            ).onSuccess {
                                dialogMessage = it
                                selectedToCancel = null
                                load()
                            }.onFailure {
                                error = it.message ?: "No se pudo cancelar la operación."
                                selectedToCancel = null
                            }
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedToCancel = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (dialogMessage != null) {
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            title = { Text("Cancelación realizada") },
            text = { Text(dialogMessage!!) },
            confirmButton = {
                TextButton(onClick = { dialogMessage = null }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
private fun ActiveTransactionCard(
    item: ActiveTradeUi,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (item.type == ActiveTradeType.BUY_ORDER) "Orden de compra" else "Oferta de venta",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.pairCode,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.dateText,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            InfoRow("Precio unitario", "%.4f".format(item.unitPrice))
            InfoRow("Cantidad original", "%.2f".format(item.originalQuantity))
            InfoRow("Total original", "%.2f".format(item.originalTotal))
            InfoRow("Cantidad ejecutada", "%.2f".format(item.executedQuantity))
            InfoRow("Total ejecutado", "%.2f".format(item.executedTotal))
            InfoRow("Cantidad restante", "%.2f".format(item.remainingQuantity))
            InfoRow("Total restante", "%.2f".format(item.remainingTotal))
            InfoRow("Reembolso", "%.2f %s".format(item.refundAmount, item.refundCurrency))

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar operación")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}