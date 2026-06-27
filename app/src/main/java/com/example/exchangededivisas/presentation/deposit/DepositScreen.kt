package com.example.exchangededivisas.presentation.deposit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.repository.PaymentMethodUi
import com.example.exchangededivisas.data.repository.WalletCurrencyUi
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.heightIn

@Composable
fun DepositScreen() {
    val scope = rememberCoroutineScope()
    val currentUser by AppSession.currentUser.collectAsState()

    var isInitialLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var currencies by remember { mutableStateOf<List<WalletCurrencyUi>>(emptyList()) }
    var methods by remember { mutableStateOf<List<PaymentMethodUi>>(emptyList()) }

    var selectedCurrencyCode by remember { mutableStateOf("PEN") }
    var selectedMethod by remember { mutableStateOf<PaymentMethodUi?>(null) }
    var amountText by remember { mutableStateOf("") }

    var methodMenuExpanded by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    var screenError by remember { mutableStateOf<String?>(null) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }

    val fallbackCurrencies = listOf(
        WalletCurrencyUi(11, "PEN", "Sol peruano", 0.0, false),
        WalletCurrencyUi(1, "USD", "Dólar estadounidense", 0.0, true),
        WalletCurrencyUi(2, "EUR", "Euro", 0.0, true)
    )

    val fallbackMethods = listOf(
        PaymentMethodUi(1, "PayPal", 3.50, 0.00),
        PaymentMethodUi(2, "Visa", 2.50, 0.00),
        PaymentMethodUi(3, "Yape", 0.00, 0.00)
    )

    LaunchedEffect(Unit) {
        isInitialLoading = true
        screenError = null

        val currenciesResult = runCatching {
            ExchangeRepository.getCurrencies()
        }

        val methodsResult = runCatching {
            ExchangeRepository.getPaymentMethods()
        }

        currencies = currenciesResult.getOrElse {
            screenError = "No se pudieron cargar monedas desde Supabase. Se usarán datos temporales."
            fallbackCurrencies
        }

        methods = methodsResult.getOrElse {
            val previous = screenError
            screenError = listOfNotNull(
                previous,
                "No se pudieron cargar métodos de pago desde Supabase. Se usarán datos temporales."
            ).joinToString("\n")
            fallbackMethods
        }

        selectedCurrencyCode = when {
            currencies.any { it.code == "PEN" } -> "PEN"
            currencies.isNotEmpty() -> currencies.first().code
            else -> "PEN"
        }

        selectedMethod = methods.firstOrNull {
            it.nombre.equals("Yape", ignoreCase = true)
        } ?: methods.firstOrNull()

        isInitialLoading = false
    }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isInvalidAmount = amountText.isNotBlank() && amount <= 0.0

    val method = selectedMethod

    val commission = if (method != null && amount > 0.0) {
        amount * (method.comisionPorcentaje / 100.0) + method.comisionFija
    } else {
        0.0
    }

    val totalToPay = amount + commission

    val canConfirm = !isInitialLoading &&
            !isSaving &&
            method != null &&
            selectedCurrencyCode.isNotBlank() &&
            amount > 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Depósito de dinero",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Registra depósitos en Supabase y actualiza tu billetera.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (isInitialLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()

                Text(
                    text = "Cargando datos...",
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        if (screenError != null) {
            Text(
                text = screenError!!,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        Text(
            text = "Moneda",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showCurrencyDialog = true },
            enabled = currencies.isNotEmpty() && !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedCurrencyCode)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Método de pago",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { methodMenuExpanded = true },
                enabled = methods.isNotEmpty() && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = method?.let { formatPaymentMethod(it) }
                        ?: "Seleccionar método de pago"
                )
            }

            DropdownMenu(
                expanded = methodMenuExpanded,
                onDismissRequest = { methodMenuExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                methods.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = item.nombre,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Comisión %.2f%% + %.2f".format(
                                        item.comisionPorcentaje,
                                        item.comisionFija
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            selectedMethod = item
                            methodMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = {
                amountText = it
                screenError = null
            },
            label = { Text("Monto") },
            singleLine = true,
            isError = isInvalidAmount,
            supportingText = {
                if (isInvalidAmount) {
                    Text("Monto inválido")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Resumen",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(10.dp))

                SummaryRow("Moneda", selectedCurrencyCode)
                SummaryRow("Monto", "%.2f".format(amount))
                SummaryRow("Método", method?.nombre ?: "-")

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                SummaryRow("Comisión", "%.2f".format(commission))
                SummaryRow("Total a pagar", "%.2f".format(totalToPay))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    isSaving = true
                    screenError = null

                    val selected = selectedMethod

                    if (selected == null) {
                        screenError = "Seleccione un método de pago."
                        isSaving = false
                        return@launch
                    }

                    ExchangeRepository.makeDeposit(
                        usuarioId = currentUser.usuarioId,
                        currencyCode = selectedCurrencyCode,
                        paymentMethodName = selected.nombre,
                        amount = amount
                    ).onSuccess {
                        dialogMessage = "Depósito registrado correctamente. La billetera fue actualizada."
                        amountText = ""
                    }.onFailure {
                        screenError = it.message ?: "No se pudo registrar el depósito."
                    }

                    isSaving = false
                }
            },
            enabled = canConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null
            )

            Text(
                text = if (isSaving) "Registrando..." else "Confirmar depósito",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = {
                Text("Seleccionar moneda")
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    currencies.forEach { currency ->
                        Text(
                            text = "${currency.code} - ${currency.name}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCurrencyCode = currency.code
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showCurrencyDialog = false }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (dialogMessage != null) {
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            title = {
                Text("Depósito confirmado")
            },
            text = {
                Text(dialogMessage!!)
            },
            confirmButton = {
                TextButton(
                    onClick = { dialogMessage = null }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
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

private fun formatPaymentMethod(method: PaymentMethodUi): String {
    return "%s - Comisión %.2f%% + %.2f".format(
        method.nombre,
        method.comisionPorcentaje,
        method.comisionFija
    )
}