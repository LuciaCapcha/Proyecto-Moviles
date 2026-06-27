package com.example.exchangededivisas.models

import java.io.Serializable
import java.util.*

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val currencyPair: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val total: Double = 0.0,
    val status: String = "pendiente",
    val walletCurrency: String = "",
    val createdAt: Date = Date(),
    val progress: Double = 0.0
) : Serializable

