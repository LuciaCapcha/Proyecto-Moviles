package com.example.exchangededivisas.presentation.pairs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.exchangededivisas.data.model.CurrencyPairChartData
import com.example.exchangededivisas.data.model.HistoricalPrice
import com.example.exchangededivisas.presentation.home.HistoricalChartCard
import java.time.LocalDateTime
import kotlin.random.Random

@Composable
fun PairDetailScreen(navController: NavController, code: String) {
    var selectedFilter by remember { mutableStateOf("1d") }
    var showOperations by remember { mutableStateOf(false) }

    val cleanCode = code.replace("_", "/")
    val codes = cleanCode.split("/")
    val base = codes.getOrNull(0) ?: "USD"
    val quote = codes.getOrNull(1) ?: "PEN"

    // Generar datos simulados según el filtro, igual que en el Home
    val chartData = remember(selectedFilter, code) {
        val prices = mutableListOf<HistoricalPrice>()
        val count = when(selectedFilter) {
            "1d" -> 24
            "1w" -> 7
            "1m" -> 30
            "1y" -> 12
            else -> 50
        }
        val now = LocalDateTime.now()
        
        // Semilla basada en el par para que sea consistente
        val seed = code.hashCode().toLong()
        val random = Random(seed)
        var lastPrice = 3.7 + random.nextDouble(-0.2, 0.2)

        for (i in count downTo 0) {
            val time = when(selectedFilter) {
                "1d" -> now.minusHours(i.toLong())
                "1w" -> now.minusDays(i.toLong())
                "1m" -> now.minusDays(i.toLong())
                "1y" -> now.minusMonths(i.toLong())
                else -> now.minusDays(i.toLong())
            }
            lastPrice += random.nextDouble(-0.05, 0.05)
            val buyPrice = lastPrice
            val sellPrice = lastPrice + 0.02 + random.nextDouble(0.01, 0.05)
            prices.add(HistoricalPrice(time, buyPrice, sellPrice))
        }
        CurrencyPairChartData(base, quote, prices)
    }

    val maxBuy = chartData.prices.maxByOrNull { it.buyPrice }?.buyPrice ?: 0.0
    val minSell = chartData.prices.minByOrNull { it.sellPrice }?.sellPrice ?: 0.0
    val avgMargin = chartData.prices.map { it.sellPrice - it.buyPrice }.average()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Detalle de par: $code",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Usamos el componente de gráfico del Home para consistencia
        HistoricalChartCard(
            title = "Evolución del Par",
            data = chartData,
            selectedRange = selectedFilter,
            onRangeSelected = { selectedFilter = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Card de Resumen de Precios
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mayor Precio", color = Color(0xFF2563EB), style = MaterialTheme.typography.labelMedium)
                    Text("Compra", color = Color(0xFF2563EB), style = MaterialTheme.typography.labelSmall)
                    Text("%.4f".format(maxBuy), fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Menor Precio", color = Color(0xFF16A34A), style = MaterialTheme.typography.labelMedium)
                    Text("Venta", color = Color(0xFF16A34A), style = MaterialTheme.typography.labelSmall)
                    Text("%.4f".format(minSell), fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Margen", color = Color(0xFFEA580C), style = MaterialTheme.typography.labelMedium)
                    Text("Promedio", color = Color(0xFFEA580C), style = MaterialTheme.typography.labelSmall)
                    Text("%.4f".format(avgMargin), fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { showOperations = !showOperations },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(if (showOperations) "Ocultar Operaciones" else "Mostrar Operaciones")
        }

        if (showOperations) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Órdenes de compra", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("500 $quote a 3.7200 $base", fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ofertas de venta", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Sin ofertas", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) {
                    Text("Orden Compra", fontSize = 12.sp)
                }
                Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) {
                    Text("Oferta Venta", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { navController.navigate("instantBuy") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1C2E))
                ) {
                    Text("Compra Inst.", fontSize = 12.sp)
                }
                Button(
                    onClick = { navController.navigate("ventaInmediata/$code") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1C2E))
                ) {
                    Text("Venta Inst.", fontSize = 12.sp)
                }
            }
        }
    }
}
