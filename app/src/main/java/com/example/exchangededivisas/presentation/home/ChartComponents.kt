package com.example.exchangededivisas.presentation.home

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.exchangededivisas.R
import com.example.exchangededivisas.data.model.CurrencyPairChartData
import com.example.exchangededivisas.data.model.HistoricalPrice
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.time.format.DateTimeFormatter

@Composable
fun TimeRangeSelector(
    selectedRange: String,
    onRangeSelected: (String) -> Unit
) {
    val ranges = listOf("1d", "1w", "1m", "1y", "Todo")
    Row(
        modifier = Modifier.wrapContentSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ranges.forEach { range ->
            val isSelected = range == selectedRange
            Surface(
                modifier = Modifier
                    .clickable { onRangeSelected(range) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = range,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (isSelected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun HistoricalChartCard(
    title: String,
    data: CurrencyPairChartData?,
    selectedRange: String,
    onRangeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (data == null || data.prices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay datos históricos disponibles",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "${data.baseCurrency} → ${data.quoteCurrency}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                TimeRangeSelector(selectedRange, onRangeSelected)

                Spacer(modifier = Modifier.height(16.dp))

                val filteredData = remember(data, selectedRange) { filterByRange(data, selectedRange) }
                key(selectedRange) { AndroidChart(filteredData) }
            }
        }
    }
}

@Composable
private fun AndroidChart(data: CurrencyPairChartData) {
    val blueColor = Color.BLUE
    val greenColor = Color.GREEN

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)

                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(true)
                xAxis.gridColor = Color.LTGRAY
                xAxis.setDrawLabels(true)

                axisLeft.setDrawGridLines(true)
                axisLeft.gridColor = Color.LTGRAY
                axisRight.isEnabled = false

                legend.isEnabled = true
                legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                legend.orientation = Legend.LegendOrientation.HORIZONTAL
                legend.setDrawInside(false)

                val marker = ChartMarkerView(context, R.layout.chart_tooltip, data.prices)
                marker.chartView = this
                this.marker = marker
            }
        },
        update = { chart ->
            val buyEntries = data.prices.mapIndexed { index, price ->
                Entry(index.toFloat(), price.buyPrice.toFloat())
            }
            val sellEntries = data.prices.mapIndexed { index, price ->
                Entry(index.toFloat(), price.sellPrice.toFloat())
            }

            val buyDataSet = LineDataSet(buyEntries, "Compra").apply {
                color = blueColor
                setCircleColor(blueColor)
                lineWidth = 2f
                setDrawCircles(true)
                circleRadius = 3f
                setDrawValues(false)
                setDrawCircleHole(false)
            }

            val sellDataSet = LineDataSet(sellEntries, "Venta").apply {
                color = greenColor
                setCircleColor(greenColor)
                lineWidth = 2f
                setDrawCircles(true)
                circleRadius = 3f
                setDrawValues(false)
                setDrawCircleHole(false)
            }

            chart.data = LineData(buyDataSet, sellDataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    )
}

class ChartMarkerView(
    context: Context,
    layoutResource: Int,
    private val prices: List<HistoricalPrice>
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.tvDate)
    private val tvBuyPrice: TextView = findViewById(R.id.tvBuyPrice)
    private val tvSellPrice: TextView = findViewById(R.id.tvSellPrice)
    private val tvMargin: TextView = findViewById(R.id.tvMargin)
    private val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val index = e?.x?.toInt() ?: 0
        if (index >= 0 && index < prices.size) {
            val price = prices[index]
            tvDate.text = price.timestamp.format(formatter)
            tvBuyPrice.text = "Compra: ${String.format("%.4f", price.buyPrice)}"
            tvSellPrice.text = "Venta: ${String.format("%.4f", price.sellPrice)}"
            tvMargin.text = "Margen: ${String.format("%.4f", price.sellPrice - price.buyPrice)}"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}

/** Recorta los precios según el rango seleccionado (1d, 1w, 1m, 1y, Todo). */
private fun filterByRange(
    data: CurrencyPairChartData,
    range: String
): CurrencyPairChartData {
    if (data.prices.isEmpty()) return data
    val last = data.prices.maxOf { it.timestamp }
    val cutoff = when (range) {
        "1d" -> last.minusDays(1)
        "1w" -> last.minusWeeks(1)
        "1m" -> last.minusMonths(1)
        "1y" -> last.minusYears(1)
        else -> return data // "Todo" = sin recorte
    }
    val filtered = data.prices.filter { !it.timestamp.isBefore(cutoff) }
    return data.copy(prices = if (filtered.isEmpty()) data.prices else filtered)
}