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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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

data class DepositInputUi(
    val currencyCode: String,
    val amountText: String
)

private data class DepositSummaryLine(
    val currencyCode: String,
    val amount: Double,
    val commission: Double,
    val totalToPay: Double
)

@Composable
fun DepositScreen() {
    val scope = rememberCoroutineScope()
    val currentUser by AppSession.currentUser.collectAsState()

    var isInitialLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var currencies by remember { mutableStateOf<List<WalletCurrencyUi>>(emptyList()) }
    var methods by remember { mutableStateOf<List<PaymentMethodUi>>(emptyList()) }
    var selectedMethod by remember { mutableStateOf<PaymentMethodUi?>(null) }

    val deposits = remember { mutableStateListOf(DepositInputUi("PEN", "")) }

    var methodMenuExpanded by remember { mutableStateOf(false) }
    var currencyDialogIndex by remember { mutableStateOf<Int?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

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

        val currenciesResult = runCatching { ExchangeRepository.getCurrencies() }
        val methodsResult = runCatching { ExchangeRepository.getPaymentMethods() }

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

        val defaultCurrency = defaultCurrencyCode(currencies)
        deposits.clear()
        deposits.add(DepositInputUi(defaultCurrency, ""))

        selectedMethod = methods.firstOrNull { it.nombre.equals("Yape", ignoreCase = true) }
            ?: methods.firstOrNull()

        isInitialLoading = false
    }

    val method = selectedMethod
    val summaryLines = buildDepositSummary(deposits, method)
    val hasInvalidAmounts = deposits.any {
        it.amountText.isNotBlank() && ((it.amountText.toDoubleOrNull() ?: 0.0) <= 0.0)
    }
    val allAmountsValid = deposits.isNotEmpty() && deposits.all { (it.amountText.toDoubleOrNull() ?: 0.0) > 0.0 }
    val canConfirm = !isInitialLoading && !isSaving && method != null && allAmountsValid

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
            text = "Puedes depositar una o varias monedas en una misma operación.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (isInitialLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Text(text = "Cargando datos...", modifier = Modifier.padding(start = 12.dp))
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        screenError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        Text(
            text = "Método de pago",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { methodMenuExpanded = true },
                enabled = methods.isNotEmpty() && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(method?.let { formatPaymentMethod(it) } ?: "Seleccionar método de pago")
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
                                Text(text = item.nombre, fontWeight = FontWeight.Bold)
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

        Text(
            text = "Monedas a depositar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        deposits.forEachIndexed { index, item ->
            DepositCurrencyRow(
                item = item,
                canDelete = deposits.size > 1 && !isSaving,
                onSelectCurrency = { currencyDialogIndex = index },
                onAmountChange = { newAmount -> deposits[index] = item.copy(amountText = newAmount) },
                onDelete = { deposits.removeAt(index) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedButton(
            onClick = {
                nextAvailableDepositCurrencyCode(currencies, deposits.map { it.currencyCode })?.let { code ->
                    deposits.add(DepositInputUi(code, ""))
                }
            },
            enabled = currencies.any { currency -> deposits.none { it.currencyCode == currency.code } } && !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(text = "Agregar otra moneda", modifier = Modifier.padding(start = 8.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))

        SummaryCard(
            title = "Resumen preliminar",
            methodName = method?.nombre ?: "-",
            lines = summaryLines,
            totalLabel = "Total a pagar por moneda"
        )

        if (hasInvalidAmounts) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Monto inválido",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { showConfirmDialog = true },
            enabled = canConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
            Text(
                text = if (isSaving) "Registrando..." else "Continuar",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    val selectedCurrencyIndex = currencyDialogIndex
    if (selectedCurrencyIndex != null) {
        AlertDialog(
            onDismissRequest = { currencyDialogIndex = null },
            title = { Text("Seleccionar moneda") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val usedByOtherRows = deposits.mapIndexedNotNull { index, item ->
                        if (index == selectedCurrencyIndex) null else item.currencyCode
                    }.toSet()

                    currencies
                        .filter { it.code !in usedByOtherRows }
                        .forEach { currency ->
                        Text(
                            text = "${currency.code} - ${currency.name}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = deposits[selectedCurrencyIndex]
                                    deposits[selectedCurrencyIndex] = current.copy(currencyCode = currency.code)
                                    currencyDialogIndex = null
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { currencyDialogIndex = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar depósito") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 430.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Revise el resumen antes de confirmar:")
                    Spacer(modifier = Modifier.height(10.dp))
                    SummaryContent(
                        methodName = method?.nombre ?: "-",
                        lines = summaryLines,
                        totalLabel = "Total a pagar por moneda"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        scope.launch {
                            isSaving = true
                            screenError = null

                            val selected = selectedMethod
                            if (selected == null) {
                                screenError = "Seleccione un método de pago."
                                isSaving = false
                                return@launch
                            }

                            val results = deposits.map { input ->
                                ExchangeRepository.makeDeposit(
                                    usuarioId = currentUser.usuarioId,
                                    currencyCode = input.currencyCode,
                                    paymentMethodName = selected.nombre,
                                    amount = input.amountText.toDoubleOrNull() ?: 0.0
                                )
                            }

                            val firstError = results.firstOrNull { it.isFailure }?.exceptionOrNull()
                            if (firstError != null) {
                                screenError = firstError.message ?: "No se pudo registrar el depósito."
                            } else {
                                dialogMessage = "Depósito registrado correctamente. Se generó el voucher pendiente de envío al correo registrado."
                                deposits.clear()
                                deposits.add(DepositInputUi(defaultCurrencyCode(currencies), ""))
                            }

                            isSaving = false
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Volver")
                }
            }
        )
    }

    dialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { dialogMessage = null },
            title = { Text("Depósito confirmado") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { dialogMessage = null }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
private fun DepositCurrencyRow(
    item: DepositInputUi,
    canDelete: Boolean,
    onSelectCurrency: () -> Unit,
    onAmountChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val amount = item.amountText.toDoubleOrNull() ?: 0.0
    val invalid = item.amountText.isNotBlank() && amount <= 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSelectCurrency,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(item.currencyCode)
                }

                IconButton(
                    onClick = onDelete,
                    enabled = canDelete
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar moneda")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = item.amountText,
                onValueChange = onAmountChange,
                label = { Text("Monto") },
                singleLine = true,
                isError = invalid,
                supportingText = {
                    if (invalid) Text("Monto inválido")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    methodName: String,
    lines: List<DepositSummaryLine>,
    totalLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            SummaryContent(methodName, lines, totalLabel)
        }
    }
}

@Composable
private fun SummaryContent(
    methodName: String,
    lines: List<DepositSummaryLine>,
    totalLabel: String
) {
    SummaryRow("Método", methodName)
    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
    lines.forEach { line ->
        Text(line.currencyCode, fontWeight = FontWeight.Bold)
        SummaryRow("Monto", "%.2f %s".format(line.amount, line.currencyCode))
        SummaryRow("Comisión", "%.2f %s".format(line.commission, line.currencyCode))
        SummaryRow(totalLabel, "%.2f %s".format(line.totalToPay, line.currencyCode))
        Spacer(modifier = Modifier.height(8.dp))
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
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}

private fun buildDepositSummary(
    deposits: List<DepositInputUi>,
    method: PaymentMethodUi?
): List<DepositSummaryLine> {
    return deposits.map { item ->
        val amount = item.amountText.toDoubleOrNull() ?: 0.0
        val commission = if (method != null && amount > 0.0) {
            amount * (method.comisionPorcentaje / 100.0) + method.comisionFija
        } else {
            0.0
        }
        DepositSummaryLine(
            currencyCode = item.currencyCode,
            amount = amount,
            commission = commission,
            totalToPay = amount + commission
        )
    }
}

private fun defaultCurrencyCode(currencies: List<WalletCurrencyUi>): String {
    return nextAvailableDepositCurrencyCode(currencies, emptyList()) ?: "PEN"
}

private fun nextAvailableDepositCurrencyCode(
    currencies: List<WalletCurrencyUi>,
    usedCodes: List<String>
): String? {
    val used = usedCodes.toSet()
    return currencies.firstOrNull { it.code == "PEN" && it.code !in used }?.code
        ?: currencies.firstOrNull { it.code !in used }?.code
}

private fun formatPaymentMethod(method: PaymentMethodUi): String {
    return "%s - Comisión %.2f%% + %.2f".format(
        method.nombre,
        method.comisionPorcentaje,
        method.comisionFija
    )
}
