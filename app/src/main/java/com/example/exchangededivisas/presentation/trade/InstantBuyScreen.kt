package com.example.exchangededivisas.presentation.trade

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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.repository.InstantBuyPreview
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.launch

@Composable
fun InstantBuyScreen(code: String = "PEN_USD") {
    val scope = rememberCoroutineScope()
    val currentUser by AppSession.currentUser.collectAsState()

    var amountText by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<InstantBuyPreview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }
    var isExecuting by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isValueInvalid = amountText.isNotBlank() && amount <= 0.0

    LaunchedEffect(code, amountText, currentUser.usuarioId) {
        preview = null
        error = null

        if (amount <= 0.0) return@LaunchedEffect

        isLoadingPreview = true

        ExchangeRepository.previewInstantBuy(
            usuarioId = currentUser.usuarioId,
            pairCode = code,
            amount = amount
        ).onSuccess {
            preview = it
        }.onFailure {
            error = it.message ?: "No se pudo calcular la compra inmediata."
        }

        isLoadingPreview = false
    }

    val p = preview
    val canConfirm = p != null &&
            amount > 0.0 &&
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
            text = "Compra inmediata",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Compra contra las mejores ofertas de venta disponibles en Supabase.",
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
                    text = code.replace("_", " → ").replace("/", " → "),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (p != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Saldo disponible: %.2f %s".format(p.availableBalance, p.fromCurrency),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Cantidad que desea comprar") },
            leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
            singleLine = true,
            isError = isValueInvalid,
            supportingText = {
                if (isValueInvalid) Text("Valor inválido")
            },
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

                if (amount <= 0.0) {
                    Text(
                        text = "Ingresa una cantidad positiva para calcular el total.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isLoadingPreview) {
                    Text("Calculando mejores ofertas...")
                } else if (p != null) {
                    SummaryRow("Cantidad solicitada", "%.2f %s".format(p.requestedAmount, p.toCurrency))
                    SummaryRow("Cantidad cubierta", "%.2f %s".format(p.coveredAmount, p.toCurrency))
                    SummaryRow("Total a pagar", "%.2f %s".format(p.totalToPay, p.fromCurrency))

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    if (p.minPrice == p.maxPrice) {
                        SummaryRow("Precio unitario", "%.4f %s".format(p.minPrice, p.fromCurrency))
                    } else {
                        SummaryRow("Precio mínimo", "%.4f %s".format(p.minPrice, p.fromCurrency))
                        SummaryRow("Precio máximo", "%.4f %s".format(p.maxPrice, p.fromCurrency))
                        SummaryRow("Precio promedio", "%.4f %s".format(p.avgPrice, p.fromCurrency))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }

        if (p != null && amount > 0.0 && !p.hasLiquidity) {
            Text(
                text = "Liquidez insuficiente",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }

        if (p != null && amount > 0.0 && p.hasLiquidity && !p.hasEnoughBalance) {
            Text(
                text = "Saldo insuficiente",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    isExecuting = true
                    error = null

                    ExchangeRepository.executeInstantBuy(
                        usuarioId = currentUser.usuarioId,
                        pairCode = code,
                        amount = amount
                    ).onSuccess { receipt ->
                        dialogMessage = "Compra confirmada. Compraste %.2f %s por %.2f %s."
                            .format(
                                receipt.boughtAmount,
                                receipt.toCurrency,
                                receipt.paidTotal,
                                receipt.fromCurrency
                            )
                        amountText = ""
                        preview = null
                    }.onFailure {
                        error = it.message ?: "No se pudo ejecutar la compra inmediata."
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
                text = if (isExecuting) "Ejecutando..." else "Confirmar compra",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    if (dialogMessage != null) {
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            title = { Text("Compra inmediata") },
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