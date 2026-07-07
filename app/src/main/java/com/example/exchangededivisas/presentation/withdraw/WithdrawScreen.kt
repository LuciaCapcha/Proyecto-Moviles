package com.example.exchangededivisas.presentation.withdraw

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.repository.PaymentMethodUi
import com.example.exchangededivisas.data.repository.WalletCurrencyUi
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.launch

data class WithdrawInputUi(
    val currencyCode: String,
    val monedaId: Int,
    val availableBalance: Double,
    val amountText: String
)

private data class WithdrawSummaryLine(
    val currencyCode: String,
    val amount: Double,
    val commission: Double,
    val totalToReceive: Double
)

@Composable
fun WithdrawScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by AppSession.currentUser.collectAsState()

    var walletCurrencies by remember { mutableStateOf<List<WalletCurrencyUi>>(emptyList()) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethodUi>>(emptyList()) }
    var selectedMethod by remember { mutableStateOf<PaymentMethodUi?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var screenError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val withdrawals = remember { mutableStateListOf<WithdrawInputUi>() }

    var methodMenuExpanded by remember { mutableStateOf(false) }
    var currencyDialogIndex by remember { mutableStateOf<Int?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        screenError = null
        runCatching {
            walletCurrencies = ExchangeRepository.loadWallet(currentUser.usuarioId)
                .filter { it.balance > 0.0 }
            paymentMethods = ExchangeRepository.getPaymentMethods()
            selectedMethod = paymentMethods.firstOrNull { it.nombre.equals("Yape", ignoreCase = true) }
                ?: paymentMethods.firstOrNull()

            withdrawals.clear()
            walletCurrencies.firstOrNull()?.let {
                withdrawals.add(
                    WithdrawInputUi(
                        currencyCode = it.code,
                        monedaId = it.monedaId,
                        availableBalance = it.balance,
                        amountText = ""
                    )
                )
            }
        }.onFailure {
            screenError = it.message ?: "No se pudieron cargar los datos."
        }
        isLoading = false
    }

    val method = selectedMethod
    val summaryLines = buildWithdrawSummary(withdrawals, method)
    val hasInvalidAmounts = withdrawals.any { item ->
        val amount = item.amountText.toDoubleOrNull() ?: 0.0
        item.amountText.isNotBlank() && amount <= 0.0
    }
    val hasInsufficientBalance = withdrawals.any { item ->
        val amount = item.amountText.toDoubleOrNull() ?: 0.0
        item.amountText.isNotBlank() && amount > item.availableBalance
    }
    val allAmountsValid = withdrawals.isNotEmpty() && withdrawals.all { item ->
        val amount = item.amountText.toDoubleOrNull() ?: 0.0
        amount > 0.0 && amount <= item.availableBalance
    }
    val canConfirm = !isLoading && !isSaving && method != null && allAmountsValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Retiro de dinero",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                modifier = Modifier.clickable { navController.popBackStack() }
            )
        }

        Text(
            text = "Puedes retirar una o varias monedas. Cada total se muestra por moneda.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Text("Cargando datos...", modifier = Modifier.padding(start = 12.dp))
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        screenError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (!isLoading && walletCurrencies.isEmpty()) {
            Text("No tienes saldo disponible para retirar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (walletCurrencies.isNotEmpty()) {
            Text("Método de cobro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { methodMenuExpanded = true },
                    enabled = paymentMethods.isNotEmpty() && !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(method?.let { formatPaymentMethod(it) } ?: "Seleccionar método de cobro")
                }

                DropdownMenu(
                    expanded = methodMenuExpanded,
                    onDismissRequest = { methodMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    paymentMethods.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(item.nombre, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Comisión %.2f%% + %.2f".format(item.comisionPorcentaje, item.comisionFija),
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

            Text("Monedas a retirar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            withdrawals.forEachIndexed { index, item ->
                WithdrawCurrencyRow(
                    item = item,
                    canDelete = withdrawals.size > 1 && !isSaving,
                    onSelectCurrency = { currencyDialogIndex = index },
                    onAmountChange = { withdrawals[index] = item.copy(amountText = it) },
                    onDelete = { withdrawals.removeAt(index) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = {
                    nextAvailableWithdrawCurrency(walletCurrencies, withdrawals.map { it.currencyCode })?.let { currency ->
                        withdrawals.add(
                            WithdrawInputUi(
                                currencyCode = currency.code,
                                monedaId = currency.monedaId,
                                availableBalance = currency.balance,
                                amountText = ""
                            )
                        )
                    }
                },
                enabled = walletCurrencies.any { currency -> withdrawals.none { it.currencyCode == currency.code } } && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Agregar otra moneda", modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(modifier = Modifier.height(18.dp))

            WithdrawSummaryCard(
                title = "Resumen preliminar",
                methodName = method?.nombre ?: "-",
                lines = summaryLines
            )

            if (hasInvalidAmounts || hasInsufficientBalance) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (hasInvalidAmounts) "Monto inválido" else "Saldo insuficiente",
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
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1C2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Text(
                    text = if (isSaving) "Registrando..." else "Continuar",
                    modifier = Modifier.padding(start = 8.dp),
                    color = Color.White
                )
            }
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
                    val usedByOtherRows = withdrawals.mapIndexedNotNull { index, item ->
                        if (index == selectedCurrencyIndex) null else item.currencyCode
                    }.toSet()

                    walletCurrencies
                        .filter { it.code !in usedByOtherRows }
                        .forEach { currency ->
                        Text(
                            text = "${currency.code} - saldo ${"%.2f".format(currency.balance)}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = withdrawals[selectedCurrencyIndex]
                                    withdrawals[selectedCurrencyIndex] = current.copy(
                                        currencyCode = currency.code,
                                        monedaId = currency.monedaId,
                                        availableBalance = currency.balance
                                    )
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
            title = { Text("Confirmar retiro") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 430.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Revise el resumen antes de confirmar:")
                    Spacer(modifier = Modifier.height(10.dp))
                    WithdrawSummaryContent(method?.nombre ?: "-", summaryLines)
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
                                screenError = "Seleccione un método de cobro."
                                isSaving = false
                                return@launch
                            }

                            val items = withdrawals.map {
                                ExchangeRepository.WithdrawItem(
                                    monedaId = it.monedaId,
                                    code = it.currencyCode,
                                    amount = it.amountText.toDoubleOrNull() ?: 0.0
                                )
                            }

                            ExchangeRepository.executeWithdraw(
                                usuarioId = currentUser.usuarioId,
                                items = items,
                                metodoPagoId = selected.metodoPagoId,
                                comisionPorcentaje = selected.comisionPorcentaje,
                                comisionFija = selected.comisionFija
                            ).onSuccess {
                                successMessage = "Retiro registrado correctamente. Se generó el voucher pendiente de envío."
                                Toast.makeText(context, "Retiro realizado con éxito", Toast.LENGTH_SHORT).show()
                                withdrawals.clear()
                                val updatedWallet = ExchangeRepository.loadWallet(currentUser.usuarioId).filter { it.balance > 0.0 }
                                walletCurrencies = updatedWallet
                                updatedWallet.firstOrNull()?.let { currency ->
                                    withdrawals.add(
                                        WithdrawInputUi(currency.code, currency.monedaId, currency.balance, "")
                                    )
                                }
                            }.onFailure { e ->
                                screenError = e.message ?: "No se pudo registrar el retiro."
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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

    successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { successMessage = null },
            title = { Text("Retiro confirmado") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { successMessage = null }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
private fun WithdrawCurrencyRow(
    item: WithdrawInputUi,
    canDelete: Boolean,
    onSelectCurrency: () -> Unit,
    onAmountChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val amount = item.amountText.toDoubleOrNull() ?: 0.0
    val invalidAmount = item.amountText.isNotBlank() && amount <= 0.0
    val insufficientBalance = item.amountText.isNotBlank() && amount > item.availableBalance
    val invalid = invalidAmount || insufficientBalance

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
                OutlinedButton(onClick = onSelectCurrency, modifier = Modifier.weight(1f)) {
                    Text("${item.currencyCode} · saldo ${"%.2f".format(item.availableBalance)}")
                }
                IconButton(onClick = onDelete, enabled = canDelete) {
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
                    if (invalid) Text(if (invalidAmount) "Monto inválido" else "Saldo insuficiente")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WithdrawSummaryCard(
    title: String,
    methodName: String,
    lines: List<WithdrawSummaryLine>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            WithdrawSummaryContent(methodName, lines)
        }
    }
}

@Composable
private fun WithdrawSummaryContent(methodName: String, lines: List<WithdrawSummaryLine>) {
    SummaryRow("Método", methodName)
    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
    lines.forEach { line ->
        Text(line.currencyCode, fontWeight = FontWeight.Bold)
        SummaryRow("Monto a retirar", "%.2f %s".format(line.amount, line.currencyCode))
        SummaryRow("Comisión", "%.2f %s".format(line.commission, line.currencyCode))
        SummaryRow("Total a recibir", "%.2f %s".format(line.totalToReceive, line.currencyCode))
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
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun buildWithdrawSummary(
    withdrawals: List<WithdrawInputUi>,
    method: PaymentMethodUi?
): List<WithdrawSummaryLine> {
    return withdrawals.map { item ->
        val amount = item.amountText.toDoubleOrNull() ?: 0.0
        val commission = if (method != null && amount > 0.0) {
            amount * (method.comisionPorcentaje / 100.0) + method.comisionFija
        } else {
            0.0
        }
        WithdrawSummaryLine(
            currencyCode = item.currencyCode,
            amount = amount,
            commission = commission,
            totalToReceive = kotlin.math.max(0.0, amount - commission)
        )
    }
}

private fun nextAvailableWithdrawCurrency(
    currencies: List<WalletCurrencyUi>,
    usedCodes: List<String>
): WalletCurrencyUi? {
    val used = usedCodes.toSet()
    return currencies.firstOrNull { it.code == "PEN" && it.code !in used }
        ?: currencies.firstOrNull { it.code !in used }
}

private fun formatPaymentMethod(method: PaymentMethodUi): String {
    return "%s - Comisión %.2f%% + %.2f".format(
        method.nombre,
        method.comisionPorcentaje,
        method.comisionFija
    )
}
