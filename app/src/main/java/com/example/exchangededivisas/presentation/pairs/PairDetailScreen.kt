package com.example.exchangededivisas.presentation.pairs

import android.content.Context
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.random.Random
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

class CustomMarkerView(
    context: Context,
    private val maxBuy: Float,
    private val minSell: Float,
    private val margin: Float
) : MarkerView(context, android.R.layout.simple_list_item_1) {
    private val tvContent: TextView = findViewById(android.R.id.text1)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(e.x.toLong()))
            tvContent.text = "$dateTime\n" +
                    "Mayor compra: ${if (maxBuy.isNaN()) "N/A" else "%.4f".format(maxBuy)}\n" +
                    "Menor venta: ${if (minSell.isNaN()) "N/A" else "%.4f".format(minSell)}\n" +
                    "Margen: ${if (margin.isNaN()) "N/A" else "%.4f".format(margin)}"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}

@Composable
fun PairDetailScreen(navController: NavController, code: String) {
    var selectedFilter by remember { mutableStateOf("1D") }
    var showOperations by remember { mutableStateOf(false) }

    val buyEntries = when (selectedFilter) {
        "1D" -> listOf(Entry(1f, 3.58f), Entry(2f, 3.62f), Entry(3f, 3.66f))
        "1W" -> listOf(Entry(1f, 3.55f), Entry(2f, 3.60f), Entry(3f, 3.72f))
        "1M" -> listOf(Entry(1f, 3.50f), Entry(2f, 3.65f), Entry(3f, 3.70f))
        "1Y" -> listOf(Entry(1f, 3.40f), Entry(2f, 3.60f), Entry(3f, 3.75f))
        else -> listOf(Entry(1f, 3.30f), Entry(2f, 3.55f), Entry(3f, 3.80f))
    }
    val sellEntries = buyEntries.map {
        val extra = 0.02f + Random.nextFloat() * (0.15f - 0.02f) // valor entre 0.02 y 0.15
        Entry(it.x, it.y + extra)
    }

    val maxBuy = buyEntries.maxByOrNull { it.y }?.y ?: Float.NaN
    val minSell = sellEntries.minByOrNull { it.y }?.y ?: Float.NaN
    val margin = if (!maxBuy.isNaN() && !minSell.isNaN()) maxBuy - minSell else Float.NaN

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Detalle de par: $code", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(12.dp))

        AndroidView(
            factory = { context ->
                LineChart(context).apply {
                    val buyDataSet = LineDataSet(buyEntries, "Compra").apply {
                        color = android.graphics.Color.BLUE
                        setCircleColor(android.graphics.Color.BLUE)
                        valueTextColor = android.graphics.Color.BLUE
                        lineWidth = 2f
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    }
                    val sellDataSet = LineDataSet(sellEntries, "Venta").apply {
                        color = android.graphics.Color.GREEN
                        setCircleColor(android.graphics.Color.GREEN)
                        valueTextColor = android.graphics.Color.GREEN
                        lineWidth = 2f
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    }
                    data = LineData(buyDataSet, sellDataSet)

                    description = Description().apply { text = "" }
                    legend.isEnabled = true
                    legend.form = Legend.LegendForm.LINE
                    legend.textColor = android.graphics.Color.BLACK

                    val markerView = CustomMarkerView(context, maxBuy, minSell, margin)
                    markerView.chartView = this
                    this.markerView = markerView

                    setTouchEnabled(true)
                    setPinchZoom(true)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1D", "1W", "1M", "1Y", "ALL").forEach { filter ->
                Button(
                    onClick = { selectedFilter = filter },
                    colors = if (selectedFilter == filter)
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    else ButtonDefaults.buttonColors()
                ) {
                    Text(filter)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mayor Precio", color = Color.Blue)
                    Text("Compra", color = Color.Blue)
                    Text(if (maxBuy.isNaN()) "N/A" else "%.4f".format(maxBuy), color = Color.Blue)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Menor Precio", color = Color.Green)
                    Text("Venta", color = Color.Green)
                    Text(if (minSell.isNaN()) "N/A" else "%.4f".format(minSell), color = Color.Green)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Margen", color = Color(0xFFFF8C00))
                    Text(if (margin.isNaN()) "N/A" else "%.4f".format(margin), color = Color(0xFFFF8C00))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = { showOperations = !showOperations }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showOperations) "Ocultar operaciones" else "Mostrar operaciones")
        }

        if (showOperations) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column {
                    Text("Órdenes de compra")
                    Text("500 PEN a 3.7200 USD")
                }
                Column {
                    Text("Ofertas de venta")
                    Text("Sin ofertas")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) {
                    Text("Generar Orden de Compra")
                }
                Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) {
                    Text("Generar Oferta de Venta")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { navController.navigate("instantBuy") }, modifier = Modifier.weight(1f)) {
                    Text("Compra inmediata")
                }
                Button(onClick = { navController.navigate("ventaInmediata") }, modifier = Modifier.weight(1f)) {
                    Text("Venta inmediata")
                }
            }
        }
    }
}