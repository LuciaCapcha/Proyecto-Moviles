package com.example.exchangededivisas.presentation.trade

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

data class BuyOrder(val quantity: Double, val price: Double)

@Composable
fun InstantSaleScreen(navController: NavController, pairCode: String = "USD/PEN") {
    val context = LocalContext.current
    val codes = pairCode.split("/")
    val baseCurrency = codes.getOrNull(0) ?: "USD"
    val quoteCurrency = codes.getOrNull(1) ?: "PEN"

    // Simulación de mejores órdenes de compra (Ordenadas por precio DESC)
    val buyOrders = remember {
        listOf(
            BuyOrder(10.0, 4.0000),
            BuyOrder(20.0, 3.9500),
            BuyOrder(50.0, 3.9000)
        )
    }

    // Saldo del usuario (Moneda a otorgar: baseCurrency)
    val userBaseBalance = MockCurrencyData.list.find { it.code == baseCurrency }?.balance ?: 0.0
    // Saldo de la moneda que recibe (para mostrar como en la imagen)
    val userQuoteBalance = MockCurrencyData.list.find { it.code == quoteCurrency }?.balance ?: 45725.5375

    // Estados
    var quantityText by remember { mutableStateOf("") }
    val quantity = quantityText.toDoubleOrNull() ?: 0.0

    // Cálculos en tiempo real según criterios
    var totalQuote = 0.0
    var remainingToSell = quantity
    val pricesUsed = mutableListOf<Double>()
    var liquidityInsufficient = false

    if (quantity > 0) {
        for (order in buyOrders) {
            val take = minOf(remainingToSell, order.quantity)
            totalQuote += take * order.price
            if (take > 0) pricesUsed.add(order.price)
            remainingToSell -= take
            if (remainingToSell <= 0) break
        }
        if (remainingToSell > 0) liquidityInsufficient = true
    }

    val isValidValue = quantity > 0
    val hasBalance = quantity <= userBaseBalance
    val hasLiquidity = !liquidityInsufficient && quantity > 0
    val canConfirm = isValidValue && hasBalance && hasLiquidity

    // Lógica de visualización de precio
    val priceDisplay = if (pricesUsed.isEmpty()) ""
    else if (pricesUsed.distinct().size == 1) "%.4f".format(pricesUsed[0])
    else {
        val min = pricesUsed.minOrNull() ?: 0.0
        val max = pricesUsed.maxOrNull() ?: 0.0
        val avg = totalQuote / quantity
        "Min: %.4f, Max: %.4f, Avg: %.4f".format(min, max, avg)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { navController.popBackStack() }, // Cerrar al tocar fuera
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) { } // Evitar que el clic en la card cierre
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(24.dp))
                    Text(
                        "Venta Inmediata",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input de cantidad
                Text(
                    text = "Cantidad de $baseCurrency",
                    modifier = Modifier.align(Alignment.Start),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) quantityText = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0.00") },
                    shape = RoundedCornerShape(12.dp)
                )

                // Validaciones
                if (quantityText.isNotEmpty() && !isValidValue) {
                    Text("Valor inválido", color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                }

                // Card de resultados (segunda imagen)
                if (isValidValue) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total: %.4f $quoteCurrency".format(totalQuote), fontWeight = FontWeight.Normal)
                            Text("Precio: $priceDisplay", fontSize = 14.sp, color = Color.Gray)
                            Text("Saldo disponible: %.4f $quoteCurrency".format(userQuoteBalance), fontSize = 14.sp, color = Color.Gray)
                        }
                    }

                    if (!hasBalance) {
                        Text("Saldo insuficiente", color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp))
                    }
                    if (liquidityInsufficient) {
                        Text("Liquidez insuficiente", color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón de Confirmar
                Button(
                    onClick = {
                        Toast.makeText(context, "Venta realizada con éxito. Notificación enviada al comprador.", Toast.LENGTH_LONG).show()
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = canConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canConfirm) Color(0xFF1A1C2E) else Color(0xFF8E8E93)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirmar", color = Color.White)
                }
            }
        }
    }
}
