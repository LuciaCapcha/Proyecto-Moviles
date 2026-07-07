package com.example.exchangededivisas.presentation.pairs

import android.widget.Toast
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
import com.example.exchangededivisas.data.model.CurrencyPairChartData
import com.example.exchangededivisas.data.model.HistoricalPrice
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.session.AppSession
import com.example.exchangededivisas.presentation.home.HistoricalChartCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.random.Random

@Composable
fun PairDetailScreen(navController: NavController, code: String) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val user    by AppSession.currentUser.collectAsState()

    var selectedFilter    by remember { mutableStateOf("1d") }
    var showOperations    by remember { mutableStateOf(false) }
    var showSellOfferDialog by remember { mutableStateOf(false) }
    var showBuyOrderDialog by remember { mutableStateOf(false) }

    val cleanCode = code.replace("_", "/")
    val codes     = cleanCode.split("/")
    val base      = codes.getOrNull(0) ?: "USD"
    val quote     = codes.getOrNull(1) ?: "PEN"

    // Saldos reales del usuario para operar.
    // En un par A -> B, una orden de compra compromete A y una oferta/venta inmediata compromete B.
    var baseBalance by remember { mutableStateOf(0.0) }
    var quoteBalance by remember { mutableStateOf(0.0) }

    // Gráfico: datos reales de historicopreciospar
    var chartData    by remember { mutableStateOf<CurrencyPairChartData?>(null) }
    var chartLoading by remember { mutableStateOf(true) }

    // Libro de órdenes: ofertas y órdenes activas
    var sellOffers   by remember { mutableStateOf<List<ExchangeRepository.SellOfferUi>>(emptyList()) }
    var buyOrders    by remember { mutableStateOf<List<ExchangeRepository.BuyOrderUi>>(emptyList()) }
    var offersLoading by remember { mutableStateOf(false) }
    var offersError   by remember { mutableStateOf<String?>(null) }

    // Cargar gráfico y saldo al abrir la pantalla
    LaunchedEffect(code) {
        chartLoading = true

        // Saldos reales
        val wallet = ExchangeRepository.loadWallet(user.usuarioId)
        baseBalance = wallet.find { it.code == base }?.balance ?: 0.0
        quoteBalance = wallet.find { it.code == quote }?.balance ?: 0.0

        // Historial de precios real (tabla historicopreciospar)
        ExchangeRepository.getPairChartData(code)
            .onSuccess { data ->
                chartData = if (data.prices.isNotEmpty()) data
                else generateFallbackChart(base, quote, code)
            }
            .onFailure { chartData = generateFallbackChart(base, quote, code) }

        chartLoading = false
    }

    suspend fun refreshOrderBookAndChart(showSpinner: Boolean = false) {
        if (showSpinner) offersLoading = true

        val sellResult = ExchangeRepository.getActiveSellOffers(code)
        val buyResult = ExchangeRepository.getActiveBuyOrders(code)

        if (sellResult.isSuccess && buyResult.isSuccess) {
            sellOffers = sellResult.getOrThrow()
            buyOrders = buyResult.getOrThrow()
            offersError = null
        } else {
            offersError = sellResult.exceptionOrNull()?.message ?: buyResult.exceptionOrNull()?.message
        }

        ExchangeRepository.getPairChartData(code)
            .onSuccess { data ->
                chartData = if (data.prices.isNotEmpty()) data else generateFallbackChart(base, quote, code)
            }

        offersLoading = false
    }

    // Cargar y refrescar el libro desde que se abre la pantalla.
    // Aunque el usuario oculte la sección, conservamos los datos para que Mayor/Menor/Margen no queden en N/A.
    LaunchedEffect(code) {
        refreshOrderBookAndChart(showSpinner = true)

        while (true) {
            delay(2500)
            refreshOrderBookAndChart(showSpinner = false)
        }
    }

    val minSell = sellOffers.minByOrNull { it.price }?.price ?: 0.0
    val maxBuy = buyOrders.maxByOrNull { it.price }?.price ?: 0.0

    // Diálogo de generar oferta de venta
    if (showSellOfferDialog) {
        SellOfferDialog(
            baseCurrency     = base,
            quoteCurrency    = quote,
            currentMinSell   = minSell,
            availableBalance = quoteBalance,
            onDismiss        = { showSellOfferDialog = false },
            onConfirm        = { amount, price ->
                showSellOfferDialog = false
                scope.launch {
                    ExchangeRepository.createSellOffer(user.usuarioId, code, amount, price)
                        .onSuccess {
                            quoteBalance -= amount
                            refreshOrderBookAndChart(showSpinner = false)
                            Toast.makeText(context,
                                "Oferta generada. Se envio notificacion al correo.",
                                Toast.LENGTH_LONG).show()
                        }
                        .onFailure { e ->
                            Toast.makeText(context,
                                "Error: ${e.message}",
                                Toast.LENGTH_LONG).show()
                        }
                }
            }
        )
    }

    if (showBuyOrderDialog) {
        BuyOrderDialog(
            baseCurrency     = base,
            quoteCurrency    = quote,
            currentBestBuy   = maxBuy,
            availableBalance = baseBalance,
            onDismiss        = { showBuyOrderDialog = false },
            onConfirm        = { amount, price ->
                showBuyOrderDialog = false
                scope.launch {
                    ExchangeRepository.createBuyOrder(user.usuarioId, code, amount, price)
                        .onSuccess {
                            baseBalance -= amount * price
                            refreshOrderBookAndChart(showSpinner = false)
                            Toast.makeText(context,
                                "Orden de compra generada. Se envio notificacion al correo.",
                                Toast.LENGTH_LONG).show()
                        }
                        .onFailure { e ->
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text  = "Detalle de par: $cleanCode",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // GRÁFICO — real si hay datos, ilustrativo si no
        if (chartLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            chartData?.let { data ->
                HistoricalChartCard(
                    title           = "Evolucion del Par",
                    data            = data,
                    selectedRange   = selectedFilter,
                    onRangeSelected = { selectedFilter = it }
                )
            }
        }

        // Texto grande: se calcula desde la cima real del libro de órdenes, no desde el gráfico.
        val mayorCompra = buyOrders.maxOfOrNull { it.price }
        val menorVenta = sellOffers.minOfOrNull { it.price }
        val margen = if (mayorCompra != null && menorVenta != null) menorVenta - mayorCompra else null

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BigStat("Mayor compra", mayorCompra, androidx.compose.ui.graphics.Color(0xFF2F80FF))
            BigStat("Menor venta", menorVenta, androidx.compose.ui.graphics.Color(0xFF22C55E))
            BigStat("Margen", margen, androidx.compose.ui.graphics.Color(0xFFFF9800))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick  = { showOperations = !showOperations },
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape    = RoundedCornerShape(8.dp)
        ) {
            Text(if (showOperations) "Ocultar Operaciones" else "Mostrar Operaciones")
        }

        if (showOperations) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Libro de Órdenes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

            // Libro de órdenes real
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ordenes de compra", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2F80FF))
                    when {
                        offersLoading -> CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        offersError != null -> Text("-", fontSize = 12.sp, color = Color.Gray)
                        buyOrders.isEmpty() -> Text("Sin órdenes", fontSize = 12.sp, color = Color.Gray)
                        else -> buyOrders.take(5).forEach { order ->
                            Text(
                                "${"%.4f".format(order.price)} $quote — ${"%.2f".format(order.quantity)} $base",
                                fontSize = 11.sp, color = Color.DarkGray
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ofertas de venta", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF22C55E))
                    when {
                        offersLoading -> CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        offersError != null -> Text("Error: $offersError", fontSize = 12.sp, color = Color.Red)
                        sellOffers.isEmpty() -> Text("Sin ofertas", fontSize = 12.sp, color = Color.Gray)
                        else -> sellOffers.take(5).forEach { offer ->
                            Text(
                                "${"%.4f".format(offer.price)} $quote — ${"%.2f".format(offer.quantity)} $base",
                                fontSize = 11.sp, color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showBuyOrderDialog = true },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("Orden Compra", fontSize = 11.sp)
                }
                Button(onClick = { showSellOfferDialog = true },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("Generar Oferta de Venta", fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick  = { navController.navigate("instantBuy/$code") },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1C2E)),
                    shape    = RoundedCornerShape(8.dp)
                ) { Text("Compra Inst.", fontSize = 11.sp) }
                Button(
                    onClick  = { navController.navigate("ventaInmediata/$code") },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1C2E)),
                    shape    = RoundedCornerShape(8.dp)
                ) { Text("Venta Inst.", fontSize = 11.sp) }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

/** Fallback cuando historicopreciospar no tiene datos para el par */
private fun generateFallbackChart(base: String, quote: String, code: String): CurrencyPairChartData {
    val random = Random(code.hashCode().toLong())
    var last   = 3.7 + random.nextDouble(-0.2, 0.2)
    val prices = (24 downTo 0).map { i ->
        last += random.nextDouble(-0.05, 0.05)
        HistoricalPrice(LocalDateTime.now().minusHours(i.toLong()), last, last + 0.04)
    }
    return CurrencyPairChartData(base, quote, prices)
}

@Composable
fun SellOfferDialog(
    baseCurrency: String,
    quoteCurrency: String,
    currentMinSell: Double,
    availableBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, price: Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var priceText  by remember { mutableStateOf("%.4f".format(currentMinSell)) }

    val amount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val price  = priceText.replace(",", ".").toDoubleOrNull()  ?: 0.0
    val total  = amount * price

    val isAmountValid = amount > 0
    val isPriceValid  = price > 0
    val hasBalance    = amount <= availableBalance
    val canConfirm    = isAmountValid && isPriceValid && hasBalance

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Generar Oferta de Venta", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text("Cantidad de $quoteCurrency a vender", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("0.00") },
                        isError = (amountText.isNotBlank() && !isAmountValid) || (isAmountValid && !hasBalance)
                    )
                    if (amountText.isNotBlank() && !isAmountValid)
                        Text("Valor invalido", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    else if (isAmountValid && !hasBalance)
                        Text("Saldo insuficiente", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Precio unitario ($baseCurrency por $quoteCurrency)", style = MaterialTheme.typography.labelMedium)
                        Text("Menor venta actual: %.4f".format(currentMinSell),
                            style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    OutlinedTextField(
                        value = priceText, onValueChange = { priceText = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = priceText.isNotBlank() && !isPriceValid
                    )
                    if (priceText.isNotBlank() && !isPriceValid)
                        Text("Valor invalido", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F5)),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total esperado: %.4f $baseCurrency".format(total), fontWeight = FontWeight.Bold)
                        Text("Saldo disponible: %.4f $quoteCurrency".format(availableBalance),
                            fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(amount, price) },
                enabled  = canConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (canConfirm) Color(0xFF1A1C2E) else Color(0xFF8E8E93),
                    contentColor   = Color.White
                )
            ) { Text("Confirmar") }
        }
    )
}

@Composable
private fun BigStat(label: String, value: Double?, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(
            value?.let { "%.4f".format(it) } ?: "N/A",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

@Composable
fun BuyOrderDialog(
    baseCurrency: String,
    quoteCurrency: String,
    currentBestBuy: Double,
    availableBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, price: Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var priceText  by remember { mutableStateOf("%.4f".format(currentBestBuy)) }

    val amount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val price  = priceText.replace(",", ".").toDoubleOrNull()  ?: 0.0
    val total  = amount * price

    val isAmountValid = amount > 0
    val isPriceValid  = price > 0
    val hasBalance    = total <= availableBalance
    val canConfirm    = isAmountValid && isPriceValid && hasBalance

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Generar Orden de Compra", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text("Cantidad de $quoteCurrency a comprar", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("0.00") },
                        isError = (amountText.isNotBlank() && !isAmountValid) || (isAmountValid && isPriceValid && !hasBalance)
                    )
                    if (amountText.isNotBlank() && !isAmountValid)
                        Text("Valor invalido", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    else if (isAmountValid && isPriceValid && !hasBalance)
                        Text("Saldo insuficiente", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Precio unitario ($baseCurrency por $quoteCurrency)", style = MaterialTheme.typography.labelMedium)
                        Text("Mayor compra actual: %.4f".format(currentBestBuy),
                            style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    OutlinedTextField(
                        value = priceText, onValueChange = { priceText = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = priceText.isNotBlank() && !isPriceValid
                    )
                    if (priceText.isNotBlank() && !isPriceValid)
                        Text("Valor invalido", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F5)),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total a comprometer: %.4f $baseCurrency".format(total), fontWeight = FontWeight.Bold)
                        Text("Saldo disponible: %.4f $baseCurrency".format(availableBalance),
                            fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(amount, price) },
                enabled  = canConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (canConfirm) Color(0xFF1A1C2E) else Color(0xFF8E8E93),
                    contentColor   = Color.White
                )
            ) { Text("Confirmar") }
        }
    )
}