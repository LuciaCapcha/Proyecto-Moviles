package com.example.exchangededivisas.presentation.withdraw

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.navigation.NavController
import com.example.exchangededivisas.data.model.MockCurrencyData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawScreen(navController: NavController) {
    var metodoSeleccionado by remember { mutableStateOf("PayPal") }
    var menuMetodosDesplegado by remember { mutableStateOf(false) }

    val metodosPago = listOf("PayPal", "Visa", "Yape")
    val comisiones = mapOf("PayPal" to 5.0, "Visa" to 3.5, "Yape" to 0.0)

    val monedasSeleccionadas = remember { mutableStateMapOf<String, Boolean>() }
    val montosIngresados = remember { mutableStateMapOf<String, String>() }

    var mensajeError by remember { mutableStateOf("") }
    var mostrarVoucher by remember { mutableStateOf(false) }

    if (monedasSeleccionadas.isEmpty()) {
        MockCurrencyData.list.forEach {
            monedasSeleccionadas[it.code] = false
            montosIngresados[it.code] = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Retiro de Fondos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Retirar Fondos de la Billetera",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "1. Selecciona el método de cobro:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = menuMetodosDesplegado,
                onExpandedChange = { menuMetodosDesplegado = !menuMetodosDesplegado },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = metodoSeleccionado,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuMetodosDesplegado) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = menuMetodosDesplegado,
                    onDismissRequest = { menuMetodosDesplegado = false }
                ) {
                    metodosPago.forEach { metodo ->
                        DropdownMenuItem(
                            text = { Text(metodo) },
                            onClick = {
                                metodoSeleccionado = metodo
                                menuMetodosDesplegado = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "2. Selecciona las monedas y montos a retirar:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            MockCurrencyData.list.forEach { currency ->
                val estaMarcado = monedasSeleccionadas[currency.code] ?: false
                val montoActual = montosIngresados[currency.code] ?: ""

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = estaMarcado,
                                onCheckedChange = { marcado ->
                                    monedasSeleccionadas[currency.code] = marcado
                                    if (!marcado) montosIngresados[currency.code] = ""
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "${currency.code} - ${currency.name}", fontWeight = FontWeight.Bold)
                                Text(text = "Saldo disponible: ${"%.2f".format(currency.balance)}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        if (estaMarcado) {
                            OutlinedTextField(
                                value = montoActual,
                                onValueChange = { montosIngresados[currency.code] = it },
                                label = { Text("Monto a retirar en ${currency.code}") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            if (mensajeError.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = mensajeError, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val ningunaSeleccionada = monedasSeleccionadas.values.none { it }
                    if (ningunaSeleccionada) {
                        mensajeError = "Debes seleccionar al menos una moneda."
                        return@Button
                    }

                    var algunMontoInvalido = false
                    var saldoInsuficiente = false

                    monedasSeleccionadas.forEach { (code, marcado) ->
                        if (marcado) {
                            val montoDouble = montosIngresados[code]?.toDoubleOrNull()
                            val originalCurrency = MockCurrencyData.list.firstOrNull { it.code == code }

                            if (montoDouble == null || montoDouble <= 0) {
                                algunMontoInvalido = true
                            } else if (originalCurrency != null && montoDouble > originalCurrency.balance) {
                                saldoInsuficiente = true
                            }
                        }
                    }

                    if (algunMontoInvalido) {
                        mensajeError = "Monto inválido"
                    } else if (saldoInsuficiente) {
                        mensajeError = "Saldo insuficiente en una o más monedas."
                    } else {
                        mensajeError = ""
                        mostrarVoucher = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Revisar Retiro", fontSize = 16.sp)
            }
        }
    }

    if (mostrarVoucher) {
        val comisionFija = comisiones[metodoSeleccionado] ?: 0.0

        AlertDialog(
            onDismissRequest = { mostrarVoucher = false },
            title = { Text("Resumen del Retiro") },
            text = {
                Column {
                    Text(text = "Método de cobro: $metodoSeleccionado", fontWeight = FontWeight.Medium)
                    Text(text = "Comisión del servicio: $ $comisionFija USD", color = Color(0xFFEF4444), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Desglose por Moneda:", fontWeight = FontWeight.Bold)
                    monedasSeleccionadas.forEach { (code, marcado) ->
                        if (marcado) {
                            val monto = montosIngresados[code]?.toDoubleOrNull() ?: 0.0
                            Text(text = "• $code: ${"%.2f".format(monto)}", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Al confirmar, el voucher se enviará a tu correo registrado y el saldo se actualizará.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        monedasSeleccionadas.forEach { (code, marcado) ->
                            if (marcado) {
                                val montoADescontar = montosIngresados[code]?.toDoubleOrNull() ?: 0.0
                                val index = MockCurrencyData.list.indexOfFirst { it.code == code }

                                if (index != -1) {
                                    try {
                                        val moneda = MockCurrencyData.list[index]
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }

                        mostrarVoucher = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Confirmar Retiro")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarVoucher = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}