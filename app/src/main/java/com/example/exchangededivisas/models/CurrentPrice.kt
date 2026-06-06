package com.example.exchangededivisas.models

import java.io.Serializable

data class CurrentPrice(
    val currencyPair: String = "",
    val highestBuyPrice: Double = 0.0,
    val lowestSellPrice: Double = 0.0,
    val totalLiquidity: Double = 0.0
) : Serializable

