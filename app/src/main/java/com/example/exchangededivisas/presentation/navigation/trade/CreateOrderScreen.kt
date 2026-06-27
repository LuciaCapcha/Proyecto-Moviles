package com.example.exchangededivisas.presentation.trade

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.api.OrderService
import com.example.exchangededivisas.models.CurrentPrice
import com.example.exchangededivisas.models.Order
import com.example.exchangededivisas.models.Wallet
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun CreateOrderScreen() {
    var quantity by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }
    var total by remember { mutableStateOf(0.0) }
    var validationMessage by remember { mutableStateOf("") }
    var isValidOrder by remember { mutableStateOf(false) }
    var highestPrice by remember { mutableStateOf(0.0) }
    var userWallet by remember { mutableStateOf<Wallet?>(null) }
    var currentPrice by remember { mutableStateOf<CurrentPrice?>(null) }

    val orderService = ApiClient.getClient().create(OrderService::class.java)
    val userId = "user_123" // Obtener del usuario autenticado
    val currencyPair = "USD/PEN"

    LaunchedEffect(Unit) {
        // Cargar precio actual
        orderService.getCurrentPrice(currencyPair).enqueue(object : Callback<CurrentPrice> {
            override fun onResponse(call: Call<CurrentPrice>, response: Response<CurrentPrice>) {
                if (response.isSuccessful) {
                    currentPrice = response.body()
                    highestPrice = response.body()?.highestBuyPrice ?: 0.0
                    unitPrice = highestPrice.toString()
                }
            }
            override fun onFailure(call: Call<CurrentPrice>, t: Throwable) {}
        })

        // Cargar wallet
        orderService.getWallet(userId).enqueue(object : Callback<Wallet> {
            override fun onResponse(call: Call<Wallet>, response: Response<Wallet>) {
                if (response.isSuccessful) {
                    userWallet = response.body()
                }
            }
            override fun onFailure(call: Call<Wallet>, t: Throwable) {}
        })
    }

    // Calcular total y validar
    LaunchedEffect(quantity, unitPrice) {
        val qty = quantity.toDoubleOrNull() ?: 0.0
        val price = unitPrice.toDoubleOrNull() ?: 0.0
        total = qty * price

        // Validar
        when {
            quantity.isEmpty() || unitPrice.isEmpty() -> {
                validationMessage = ""
                isValidOrder = false
            }
            qty <= 0 || price <= 0 -> {
                validationMessage = "❌ Valor inválido"
                isValidOrder = false
            }
            userWallet != null && total > (userWallet!!.balance - userWallet!!.committedAmount) -> {
                validationMessage = "❌ Saldo insuficiente"
                isValidOrder = false
            }
            currentPrice != null && qty > currentPrice!!.totalLiquidity -> {
                validationMessage = "❌ Liquidez insuficiente"
                isValidOrder = false
            }
            else -> {
                validationMessage = "✅ Orden válida"
                isValidOrder = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Generar Orden de Compra",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Text(
            text = "Precio más alto: S/. $highestPrice",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 15.dp)
        )

        // Cantidad
        Text("Cantidad:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Ingresa la cantidad") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp),
            singleLine = true
        )

        // Precio unitario
        Text("Precio unitario:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = unitPrice,
            onValueChange = { unitPrice = it },
            label = { Text("Precio unitario") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp),
            singleLine = true
        )

        // Total
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            color = Color(0xFFE8F5E9),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Total: S/. %.2f".format(total),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Mensaje de validación
        if (validationMessage.isNotEmpty()) {
            Text(
                text = validationMessage,
                fontSize = 14.sp,
                color = if (validationMessage.contains("✅")) Color.Green else Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Botón confirmar
        Button(
            onClick = {
                val order = Order(
                    userId = userId,
                    currencyPair = currencyPair,
                    quantity = quantity.toDouble(),
                    unitPrice = unitPrice.toDouble(),
                    total = total,
                    walletCurrency = userWallet?.currency ?: "PEN",
                    status = "pendiente"
                )
                // Aquí enviar la orden
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = isValidOrder
        ) {
            Text("Confirmar Orden", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

