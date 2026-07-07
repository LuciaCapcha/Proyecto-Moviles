package com.example.exchangededivisas.presentation.home

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

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
                modifier = Modifier.clickable { onRangeSelected(range) },
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

                TimeRangeSelector(
                    selectedRange = selectedRange,
                    onRangeSelected = onRangeSelected
                )

                Spacer(modifier = Modifier.height(16.dp))

                val chartState = remember(data, selectedRange) {
                    buildChartState(data, selectedRange)
                }

                key(selectedRange, chartState.points.size, chartState.intervalMinutes) {
                    AndroidChart(chartState)
                }
            }
        }
    }
}

@Composable
private fun AndroidChart(
    state: ChartState
) {
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
                xAxis.granularity = 1f

                axisLeft.setDrawGridLines(true)
                axisLeft.gridColor = Color.LTGRAY
                axisRight.isEnabled = false

                legend.isEnabled = true
                legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                legend.orientation = Legend.LegendOrientation.HORIZONTAL
                legend.setDrawInside(false)
            }
        },
        update = { chart ->
            val buyEntries = state.points.map { point ->
                Entry(point.x, point.price.buyPrice.toFloat())
            }

            val sellEntries = state.points.map { point ->
                Entry(point.x, point.price.sellPrice.toFloat())
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

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val slot = value.roundToInt().coerceAtLeast(0)
                    val timestamp = state.firstBucket.plusMinutes(slot.toLong() * state.intervalMinutes)
                    return timestamp.format(state.axisFormatter)
                }
            }

            val marker = ChartMarkerView(
                context = chart.context,
                layoutResource = R.layout.chart_tooltip,
                points = state.points
            )
            marker.chartView = chart
            chart.marker = marker

            chart.data = LineData(buyDataSet, sellDataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    )
}

private class ChartMarkerView(
    context: Context,
    layoutResource: Int,
    private val points: List<ChartPoint>
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.tvDate)
    private val tvBuyPrice: TextView = findViewById(R.id.tvBuyPrice)
    private val tvSellPrice: TextView = findViewById(R.id.tvSellPrice)
    private val tvMargin: TextView = findViewById(R.id.tvMargin)

    private val minuteFormatter = DateTimeFormatter.ofPattern("dd/MM\nHH:mm")

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val x = e?.x ?: 0f
        val point = points.minByOrNull { abs(it.x - x) }

        if (point != null) {
            val price = point.price
            tvDate.text = price.timestamp.format(minuteFormatter)
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

private data class ChartPoint(
    val x: Float,
    val price: HistoricalPrice
)

private data class ChartState(
    val points: List<ChartPoint>,
    val firstBucket: LocalDateTime,
    val intervalMinutes: Long,
    val axisFormatter: DateTimeFormatter
)

private data class BucketConfig(
    val intervalMinutes: Long,
    val rangeStart: (LocalDateTime) -> LocalDateTime?,
    val axisFormatter: DateTimeFormatter
)

private fun buildChartState(
    data: CurrencyPairChartData,
    range: String
): ChartState {
    val config = bucketConfig(range)
    val ordered = data.prices.sortedBy { it.timestamp }
    val last = ordered.maxOf { it.timestamp }
    val cutoff = config.rangeStart(last)

    val ranged = if (cutoff == null) ordered else ordered.filter { !it.timestamp.isBefore(cutoff) }
    val safeRange = ranged.ifEmpty { ordered.takeLast(1) }

    val bucketed = safeRange
        .groupBy { bucketEnd(it.timestamp, config.intervalMinutes) }
        .mapNotNull { (bucket, group) ->
            val latest = group.maxByOrNull { it.timestamp } ?: return@mapNotNull null
            latest.copy(timestamp = bucket)
        }
        .sortedBy { it.timestamp }

    val firstBucket = bucketed.firstOrNull()?.timestamp ?: bucketEnd(last, config.intervalMinutes)
    val points = bucketed.map { price ->
        val minutes = java.time.Duration.between(firstBucket, price.timestamp).toMinutes()
        val x = (minutes.toDouble() / config.intervalMinutes.toDouble()).toFloat()
        ChartPoint(x = x, price = price)
    }

    return ChartState(
        points = points,
        firstBucket = firstBucket,
        intervalMinutes = config.intervalMinutes,
        axisFormatter = config.axisFormatter
    )
}

private fun bucketConfig(range: String): BucketConfig {
    return when (range) {
        "1d" -> BucketConfig(
            intervalMinutes = 5,
            rangeStart = { it.minusDays(1) },
            axisFormatter = DateTimeFormatter.ofPattern("HH:mm")
        )

        "1w" -> BucketConfig(
            intervalMinutes = 60,
            rangeStart = { it.minusWeeks(1) },
            axisFormatter = DateTimeFormatter.ofPattern("dd/MM\nHH:mm")
        )

        "1m" -> BucketConfig(
            intervalMinutes = 360,
            rangeStart = { it.minusMonths(1) },
            axisFormatter = DateTimeFormatter.ofPattern("dd/MM")
        )

        "1y" -> BucketConfig(
            intervalMinutes = 10080,
            rangeStart = { it.minusYears(1) },
            axisFormatter = DateTimeFormatter.ofPattern("dd/MM")
        )

        else -> BucketConfig(
            intervalMinutes = 1440,
            rangeStart = { null },
            axisFormatter = DateTimeFormatter.ofPattern("dd/MM")
        )
    }
}

private fun bucketEnd(
    timestamp: LocalDateTime,
    intervalMinutes: Long
): LocalDateTime {
    val epochMinutes = timestamp.toEpochSecond(ZoneOffset.UTC) / 60L
    val bucket = ceil(epochMinutes.toDouble() / intervalMinutes.toDouble()).toLong() * intervalMinutes
    return LocalDateTime.ofEpochSecond(bucket * 60L, 0, ZoneOffset.UTC)
}
