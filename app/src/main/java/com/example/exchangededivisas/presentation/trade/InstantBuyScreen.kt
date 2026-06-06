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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

data class SellOffer(
    val seller: String,
    val amountAvailable: Double,
    val unitPrice: Double
)

data class InstantBuyCalculation(
    val requestedAmount: Double,
    val coveredAmount: Double,
    val total: Double,
    val prices: List<Double>
)

private val mockSellOffers = listOf(
    SellOffer("vendedor_01", 300.0, 3.72),
    SellOffer("vendedor_02", 250.0, 3.72),
    SellOffer("vendedor_03", 400.0, 3.75),
    SellOffer("vendedor_04", 600.0, 3.78),
    SellOffer("vendedor_05", 350.0, 3.80)
).sortedBy { it.unitPrice }

private fun calculateInstantBuy(amount: Double): InstantBuyCalculation {
    var remaining = amount
    var covered = 0.0
    var total = 0.0
    val usedPrices = mutableListOf<Double>()

    for (offer in mockSellOffers) {
        if (remaining <= 0.0) break

        val taken = minOf(remaining, offer.amountAvailable)
        covered += taken
        total += taken * offer.unitPrice
        usedPrices.add(offer.unitPrice)
        remaining -= taken
    }

    return InstantBuyCalculation(
        requestedAmount = amount,
        coveredAmount = covered,
        total = total,
        prices = usedPrices
    )
}

@Composable
fun InstantBuyScreen() {
    var amountText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    val userPenBalance = 2800.00
    val fromCurrency = "PEN"
    val toCurrency = "USD"

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val calculation = calculateInstantBuy(amount)

    val isValueInvalid = amountText.isNotBlank() && amount <= 0.0
    val hasLiquidity = amount > 0.0 && calculation.coveredAmount >= amount
    val hasEnoughBalance = userPenBalance >= calculation.total

    val canConfirm = amount > 0.0 && hasLiquidity && hasEnoughBalance

    val minPrice = calculation.prices.minOrNull() ?: 0.0
    val maxPrice = calculation.prices.maxOrNull() ?: 0.0
    val avgPrice = if (calculation.coveredAmount > 0.0) {
        calculation.total / calculation.coveredAmount
    } else {
        0.0
    }

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
            text = "Compra $toCurrency usando $fromCurrency contra las mejores ofertas de venta disponibles.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Par seleccionado",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$fromCurrency → $toCurrency",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Saldo disponible: %.2f %s".format(userPenBalance, fromCurrency),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Cantidad que desea comprar en $toCurrency") },
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
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cálculo en tiempo real",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                SummaryRow("Cantidad solicitada", "%.2f %s".format(amount, toCurrency))
                SummaryRow("Cantidad cubierta", "%.2f %s".format(calculation.coveredAmount, toCurrency))
                SummaryRow("Total a pagar", "%.2f %s".format(calculation.total, fromCurrency))

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                if (amount <= 0.0) {
                    Text(
                        text = "Ingresa una cantidad para calcular el precio.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (calculation.prices.isNotEmpty() && minPrice == maxPrice) {
                    SummaryRow("Precio unitario", "%.2f %s".format(minPrice, fromCurrency))
                } else if (calculation.prices.isNotEmpty()) {
                    SummaryRow("Precio mínimo", "%.2f %s".format(minPrice, fromCurrency))
                    SummaryRow("Precio máximo", "%.2f %s".format(maxPrice, fromCurrency))
                    SummaryRow("Precio promedio", "%.2f %s".format(avgPrice, fromCurrency))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (amount > 0.0 && !hasLiquidity) {
            Text(
                text = "Liquidez insuficiente",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }

        if (amount > 0.0 && hasLiquidity && !hasEnoughBalance) {
            Text(
                text = "Saldo insuficiente",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { showDialog = true },
            enabled = canConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Text(
                text = "Confirmar compra",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Compra confirmada") },
            text = {
                Text(
                    "Se compraron %.2f %s por un total de %.2f %s. La billetera fue actualizada y el vendedor recibirá una notificación por correo."
                        .format(amount, toCurrency, calculation.total, fromCurrency)
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
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
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            fontWeight = FontWeight.Medium
        )
    }
}