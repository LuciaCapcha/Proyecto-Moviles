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
import androidx.navigation.NavController
import com.example.exchangededivisas.data.model.MockCurrencyData

data class PaymentMethod(val name: String, val commission: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawScreen(navController: NavController) {
    val context = LocalContext.current
    val currencies = MockCurrencyData.list
    val paymentMethods = listOf(
        PaymentMethod("PayPal (-5%)", 0.05),
        PaymentMethod("Yape (-2%)", 0.02),
        PaymentMethod("Plin (-2%)", 0.02),
        PaymentMethod("Transferencia Bancaria (-1%)", 0.01)
    )

    // Estados
    var selectedCurrencies by remember { mutableStateOf(setOf<String>()) }
    var amounts by remember { mutableStateOf(mapOf<String, String>()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf(paymentMethods[0]) }

    val scrollState = rememberScrollState()

    // Cálculos
    val subtotal = selectedCurrencies.sumOf { code ->
        amounts[code]?.toDoubleOrNull() ?: 0.0
    }
    val commissionValue = subtotal * selectedMethod.commission
    val totalToReceive = subtotal - commissionValue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Cabecera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Retirar Dinero",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Default.Close, 
                contentDescription = "Cerrar",
                modifier = Modifier.clickable { navController.popBackStack() }
            )
        }

        Text(
            text = "Selecciona las monedas y el método de cobro",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Monedas a Retirar",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)

        )

        // Lista de Monedas en 2 columnas
        val chunkedCurrencies = currencies.chunked(2)
        chunkedCurrencies.forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { currency ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedCurrencies.contains(currency.code),
                            onCheckedChange = { isChecked ->
                                selectedCurrencies = if (isChecked) {
                                    selectedCurrencies + currency.code
                                } else {
                                    selectedCurrencies - currency.code
                                }
                            }
                        )
                        Text(
                            text = "${currency.code} (${String.format("%.2f", currency.balance)})",
                            fontSize = 13.sp
                        )
                    }
                }
                if (pair.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (selectedCurrencies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Montos", fontWeight = FontWeight.Bold)

            selectedCurrencies.forEach { code ->
                val amount = amounts[code] ?: ""
                val isInvalid = amount.toDoubleOrNull()?.let { it > (currencies.find { c -> c.code == code }?.balance ?: 0.0) } ?: false

                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        amounts = amounts + (code to newValue)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    label = { Text("Monto en $code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isInvalid
                )
                if (isInvalid) {
                    Text("Monto excede el saldo", color = Color.Red, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Método de Cobro", fontWeight = FontWeight.Bold)
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = selectedMethod.name,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                paymentMethods.forEach { method ->
                    DropdownMenuItem(
                        text = { Text(method.name) },
                        onClick = {
                            selectedMethod = method
                            expanded = false
                        }
                    )
                }
            }
        }

        if (subtotal > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", fontSize = 14.sp)
                        Text("${String.format("%.2f", subtotal)}", fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Comisión (${(selectedMethod.commission * 100).toInt()}%):", fontSize = 14.sp)
                        Text("${String.format("%.2f", commissionValue)}", fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total a recibir:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${String.format("%.2f", totalToReceive)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                Toast.makeText(context, "Retiro realizado con éxito", Toast.LENGTH_SHORT).show()
                navController.navigate("wallet") {
                    popUpTo("home") { inclusive = false }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = subtotal > 0 && selectedCurrencies.all { code -> 
                val amount = amounts[code]?.toDoubleOrNull() ?: 0.0
                amount > 0 && amount <= (currencies.find { c -> c.code == code }?.balance ?: 0.0)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1C2E)) // Azul oscuro como la imagen
        ) {
            Text("Confirmar Retiro", color = Color.White)
        }
    }
}
