package com.example.exchangededivisas.data.model

import java.time.LocalDateTime

data class HistoricalPrice(
    val timestamp: LocalDateTime,
    val buyPrice: Double,
    val sellPrice: Double
)

data class CurrencyPairChartData(
    val baseCurrency: String,
    val quoteCurrency: String,
    val prices: List<HistoricalPrice>
) {
    val pairName: String get() = "$baseCurrency/$quoteCurrency"
}
