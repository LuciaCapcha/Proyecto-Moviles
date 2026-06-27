package com.example.exchangededivisas.presentation.withdraw

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MenuAnchorType
import androidx.navigation.NavController
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.repository.PaymentMethodUi
import com.example.exchangededivisas.data.repository.WalletCurrencyUi
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawScreen(navController: NavController) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val user    by AppSession.currentUser.collectAsState()

    var walletCurrencies by remember { mutableStateOf<List<WalletCurrencyUi>>(emptyList()) }
    var paymentMethods   by remember { mutableStateOf<List<PaymentMethodUi>>(emptyList()) }
    var isLoading        by remember { mutableStateOf(true) }
    var loadError        by remember { mutableStateOf<String?>(null) }

    var selectedCurrencyIds by remember { mutableStateOf(setOf<Int>()) }
    var amounts             by remember { mutableStateOf(mapOf<Int, String>()) }
    var expanded            by remember { mutableStateOf(false) }
    var selectedMethod      by remember { mutableStateOf<PaymentMethodUi?>(null) }
    var confirmLoading      by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching {
            walletCurrencies = ExchangeRepository.loadWallet(user.usuarioId)
                .filter { it.balance > 0.0 }
            paymentMethods = ExchangeRepository.getPaymentMethods()
            selectedMethod = paymentMethods.firstOrNull()
        }.onFailure { loadError = it.message }
        isLoading = false
    }

    val subtotal        = selectedCurrencyIds.sumOf { id -> amounts[id]?.toDoubleOrNull() ?: 0.0 }
    val comision        = selectedMethod?.comisionPorcentaje ?: 0.0
    val commissionValue = subtotal * comision
    val totalToReceive  = subtotal - commissionValue

    val allAmountsValid = selectedCurrencyIds.isNotEmpty() && selectedCurrencyIds.all { id ->
        val amt    = amounts[id]?.toDoubleOrNull() ?: 0.0
        val maxBal = walletCurrencies.find { it.monedaId == id }?.balance ?: 0.0
        amt > 0.0 && amt <= maxBal
    }
    val canConfirm = allAmountsValid && selectedMethod != null && !confirmLoading

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
            Text("Retirar Dinero",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Close, contentDescription = "Cerrar",
                modifier = Modifier.clickable { navController.popBackStack() })
        }
        Text("Selecciona las monedas y el metodo de cobro", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            loadError != null -> Text("Error al cargar datos: $loadError", color = Color.Red)
            walletCurrencies.isEmpty() -> Text("No tienes saldo disponible para retirar.", color = Color.Gray)
            else -> {
                Text("Monedas a Retirar", fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp))

                walletCurrencies.chunked(2).forEach { chunk: List<WalletCurrencyUi> ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        chunk.forEach { currency: WalletCurrencyUi ->
                            Row(modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedCurrencyIds.contains(currency.monedaId),
                                    onCheckedChange = { checked ->
                                        selectedCurrencyIds = if (checked)
                                            selectedCurrencyIds + currency.monedaId
                                        else
                                            selectedCurrencyIds - currency.monedaId
                                    }
                                )
                                Text("${currency.code} (${"%.2f".format(currency.balance)})",
                                    fontSize = 13.sp)
                            }
                        }
                        if (chunk.size < 2) Spacer(modifier = Modifier.weight(1f))
                    }
                }

                if (selectedCurrencyIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Montos", fontWeight = FontWeight.Bold)

                    selectedCurrencyIds.forEach { id: Int ->
                        val currency = walletCurrencies.find { it.monedaId == id } ?: return@forEach
                        val text     = amounts[id] ?: ""
                        val inputAmt = text.toDoubleOrNull() ?: 0.0
                        val isOver   = inputAmt > currency.balance

                        OutlinedTextField(
                            value = text,
                            onValueChange = { amounts = amounts + (id to it) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            label = { Text("Monto en ${currency.code} (max ${"%.2f".format(currency.balance)})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = isOver
                        )
                        if (isOver) Text("Monto excede el saldo", color = Color.Red, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Metodo de Cobro", fontWeight = FontWeight.Bold)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedMethod?.let {
                            "${it.nombre} (-${"%.1f".format(it.comisionPorcentaje * 100)}%)"
                        } ?: "Selecciona metodo",
                        onValueChange = {}, readOnly = true,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        paymentMethods.forEach { method: PaymentMethodUi ->
                            DropdownMenuItem(
                                text = { Text("${method.nombre} (-${"%.1f".format(method.comisionPorcentaje * 100)}%)") },
                                onClick = { selectedMethod = method; expanded = false }
                            )
                        }
                    }
                }

                if (subtotal > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5)),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", fontSize = 14.sp)
                                Text("%.2f".format(subtotal), fontWeight = FontWeight.Medium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Comision (${(comision * 100).toInt()}%):", fontSize = 14.sp)
                                Text("%.2f".format(commissionValue), fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total a recibir:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("%.2f".format(totalToReceive), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            confirmLoading = true
                            val method = selectedMethod ?: return@launch
                            val items = selectedCurrencyIds.mapNotNull { id: Int ->
                                val currency = walletCurrencies.find { it.monedaId == id } ?: return@mapNotNull null
                                val amt      = amounts[id]?.toDoubleOrNull()              ?: return@mapNotNull null
                                ExchangeRepository.WithdrawItem(id, currency.code, amt)
                            }
                            ExchangeRepository.executeWithdraw(
                                usuarioId          = user.usuarioId,
                                items              = items,
                                metodoPagoId       = method.metodoPagoId,
                                comisionPorcentaje = method.comisionPorcentaje
                            ).onSuccess {
                                Toast.makeText(context, "Retiro realizado con exito", Toast.LENGTH_SHORT).show()
                                navController.navigate("wallet") { popUpTo("home") { inclusive = false } }
                            }.onFailure { e ->
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                            confirmLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled  = canConfirm,
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1C2E)),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    if (confirmLoading)
                        CircularProgressIndicator(modifier = Modifier.size(20.dp),
                            color = Color.White, strokeWidth = 2.dp)
                    else
                        Text("Confirmar Retiro", color = Color.White)
                }
            }
        }
    }
}
