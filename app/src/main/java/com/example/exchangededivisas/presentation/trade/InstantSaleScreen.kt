package com.example.exchangededivisas.presentation.trade

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InstantSaleScreen(navController: NavController, pairCode: String = "USD_PEN") {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val user by AppSession.currentUser.collectAsState()

    val cleanCode = pairCode.replace("/", "_")
    var quantityText by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<ExchangeRepository.InstantSalePreview?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }
    var isExecuting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastRefreshText by remember { mutableStateOf("Sin cálculo") }

    val quantity = quantityText.toDoubleOrNull() ?: 0.0
    val isValueInvalid = quantityText.isNotBlank() && quantity <= 0.0

    suspend fun refreshPreview(showLoading: Boolean) {
        if (quantity <= 0.0) {
            preview = null
            lastRefreshText = "Sin cálculo"
            return
        }

        if (showLoading) isLoadingPreview = true
        error = null

        ExchangeRepository.previewInstantSale(
            usuarioId = user.usuarioId,
            pairCode = cleanCode,
            amount = quantity
        ).onSuccess {
            preview = it
            lastRefreshText = "Actualizado automáticamente"
        }.onFailure {
            preview = null
            error = it.message ?: "No se pudo calcular la venta inmediata."
            lastRefreshText = "No actualizado"
        }

        if (showLoading) isLoadingPreview = false
    }

    LaunchedEffect(cleanCode, quantityText, user.usuarioId) {
        preview = null
        error = null

        if (quantity <= 0.0) return@LaunchedEffect

        refreshPreview(showLoading = true)

        while (true) {
            delay(2500)
            if (!isExecuting) refreshPreview(showLoading = false)
        }
    }

    val p = preview
    val canConfirm = p != null &&
            quantity > 0.0 &&
            p.hasLiquidity &&
            p.hasEnoughBalance &&
            !isExecuting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Venta inmediata",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Vende contra las mejores órdenes de compra disponibles. El cálculo se refresca solo para evitar choques entre operaciones.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Par seleccionado", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = cleanCode.replace("_", " → "),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(lastRefreshText, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (p != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Saldo disponible: %.2f %s".format(p.availableBalance, p.quoteCurrency),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it },
            label = { Text("Cantidad que desea vender") },
            leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null) },
            singleLine = true,
            isError = isValueInvalid,
            supportingText = { if (isValueInvalid) Text("Valor inválido") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cálculo en tiempo real",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (quantity <= 0.0) {
                    Text(
                        text = "Ingresa una cantidad positiva para calcular el total.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isLoadingPreview) {
                    Text("Calculando mejores órdenes...")
                } else if (p != null) {
                    SummaryRow("Cantidad solicitada", "%.2f %s".format(p.requestedAmount, p.quoteCurrency))
                    SummaryRow("Cantidad cubierta", "%.2f %s".format(p.coveredAmount, p.quoteCurrency))
                    SummaryRow("Total a recibir", "%.2f %s".format(p.totalToReceive, p.baseCurrency))

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    if (p.minPrice == p.maxPrice) {
                        SummaryRow("Precio unitario", "%.4f %s".format(p.avgPrice, p.baseCurrency))
                    } else {
                        SummaryRow("Precio mínimo", "%.4f %s".format(p.minPrice, p.baseCurrency))
                        SummaryRow("Precio máximo", "%.4f %s".format(p.maxPrice, p.baseCurrency))
                        SummaryRow("Precio promedio", "%.4f %s".format(p.avgPrice, p.baseCurrency))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }

        if (p != null && quantity > 0.0 && !p.hasLiquidity) {
            Text("Liquidez insuficiente", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }

        if (p != null && quantity > 0.0 && p.hasLiquidity && !p.hasEnoughBalance) {
            Text("Saldo insuficiente", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    isExecuting = true
                    error = null

                    val freshPreview = ExchangeRepository.previewInstantSale(
                        usuarioId = user.usuarioId,
                        pairCode = cleanCode,
                        amount = quantity
                    ).getOrElse {
                        error = it.message ?: "No se pudo recalcular la venta inmediata."
                        isExecuting = false
                        return@launch
                    }

                    preview = freshPreview

                    if (!freshPreview.hasLiquidity) {
                        error = "Liquidez insuficiente"
                        isExecuting = false
                        return@launch
                    }

                    if (!freshPreview.hasEnoughBalance) {
                        error = "Saldo insuficiente"
                        isExecuting = false
                        return@launch
                    }

                    ExchangeRepository.executeInstantSale(
                        usuarioId = user.usuarioId,
                        pairCode = cleanCode,
                        amount = quantity
                    ).onSuccess { receipt ->
                        Toast.makeText(
                            context,
                            "Venta confirmada. Vendiste %.2f %s por %.2f %s.".format(
                                receipt.soldAmount,
                                receipt.quoteCurrency,
                                receipt.receivedTotal,
                                receipt.baseCurrency
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                        navController.popBackStack()
                    }.onFailure {
                        error = it.message ?: "No se pudo ejecutar la venta inmediata. El libro pudo cambiar antes de confirmar."
                        refreshPreview(showLoading = false)
                    }

                    isExecuting = false
                }
            },
            enabled = canConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Text(
                text = if (isExecuting) "Ejecutando..." else "Confirmar venta",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
