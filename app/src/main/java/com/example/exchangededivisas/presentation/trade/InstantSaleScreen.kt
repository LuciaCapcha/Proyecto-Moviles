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
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.launch

data class BuyOrder(val quantity: Double, val price: Double)

@Composable
fun InstantSaleScreen(navController: NavController, pairCode: String = "USD_PEN") {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val user    by AppSession.currentUser.collectAsState()

    // Normalizar código: el navGraph lo pasa con "_", parsePairCode espera "_" o "/"
    val cleanCode     = pairCode.replace("/", "_")
    val codes         = cleanCode.split("_")
    val baseCurrency  = codes.getOrNull(0) ?: "USD"
    val quoteCurrency = codes.getOrNull(1) ?: "PEN"

    var quantityText    by remember { mutableStateOf("") }
    var preview         by remember { mutableStateOf<ExchangeRepository.InstantSalePreview?>(null) }
    var previewLoading  by remember { mutableStateOf(false) }
    var confirmLoading  by remember { mutableStateOf(false) }
    var errorMsg        by remember { mutableStateOf<String?>(null) }

    val quantity = quantityText.toDoubleOrNull() ?: 0.0

    // Preview en tiempo real: cada vez que cambia la cantidad consultamos Supabase
    LaunchedEffect(quantityText) {
        if (quantity <= 0.0) { preview = null; return@LaunchedEffect }
        previewLoading = true
        ExchangeRepository.previewInstantSale(user.usuarioId, cleanCode, quantity)
            .onSuccess { preview = it; errorMsg = null }
            .onFailure { preview = null; errorMsg = it.message }
        previewLoading = false
    }

    val canConfirm = preview?.let { it.hasLiquidity && it.hasEnoughBalance } == true && !confirmLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { navController.popBackStack() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {}
                .padding(16.dp),
            shape  = RoundedCornerShape(16.dp),
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
                    Text("Venta Inmediata",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text     = "Cantidad de $baseCurrency",
                    modifier = Modifier.align(Alignment.Start),
                    fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) quantityText = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0.00") },
                    shape = RoundedCornerShape(12.dp)
                )

                if (quantityText.isNotEmpty() && quantity <= 0.0)
                    Text("Valor invalido", color = Color.Red, fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start))

                // Spinner mientras calcula
                if (previewLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // Resultados del preview real
                preview?.let { p ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5)),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Recibiras: ${"%.4f".format(p.totalToReceive)} $quoteCurrency",
                                fontWeight = FontWeight.Normal)
                            val priceDisplay = if (p.minPrice == p.maxPrice)
                                "Precio: ${"%.4f".format(p.avgPrice)}"
                            else
                                "Min: ${"%.4f".format(p.minPrice)}, Max: ${"%.4f".format(p.maxPrice)}, Avg: ${"%.4f".format(p.avgPrice)}"
                            Text(priceDisplay, fontSize = 14.sp, color = Color.Gray)
                            Text("Saldo disponible: ${"%.4f".format(p.availableBalance)} $baseCurrency",
                                fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                    if (!p.hasEnoughBalance)
                        Text("Saldo insuficiente", color = Color.Red, fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start).padding(top = 4.dp))
                    if (!p.hasLiquidity)
                        Text("Liquidez insuficiente", color = Color.Red, fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start).padding(top = 4.dp))
                }

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Color.Red, fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            confirmLoading = true
                            ExchangeRepository.executeInstantSale(user.usuarioId, cleanCode, quantity)
                                .onSuccess { receipt ->
                                    Toast.makeText(context,
                                        "Venta realizada: ${"%.4f".format(receipt.soldAmount)} ${receipt.baseCurrency}" +
                                                " -> ${"%.4f".format(receipt.receivedTotal)} ${receipt.quoteCurrency}",
                                        Toast.LENGTH_LONG).show()
                                    navController.popBackStack()
                                }
                                .onFailure { e ->
                                    errorMsg = e.message ?: "Error al procesar la venta"
                                }
                            confirmLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled  = canConfirm,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (canConfirm) Color(0xFF1A1C2E) else Color(0xFF8E8E93)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (confirmLoading)
                        CircularProgressIndicator(modifier = Modifier.size(20.dp),
                            color = Color.White, strokeWidth = 2.dp)
                    else
                        Text("Confirmar", color = Color.White)
                }
            }
        }
    }
}
