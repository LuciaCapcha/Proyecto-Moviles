package com.example.exchangededivisas.models

import java.io.Serializable

data class OrderBook(
    val currencyPair: String = "", // "USD/PEN"
    val buyOrders: List<OrderItem> = emptyList(), // Órdenes de compra (derecha)
    val sellOffers: List<OrderItem> = emptyList() // Ofertas de venta (izquierda)
) : Serializable

data class OrderItem(
    val orderId: String = "",
    val userId: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val timestamp: Long = 0L
) : Serializable

