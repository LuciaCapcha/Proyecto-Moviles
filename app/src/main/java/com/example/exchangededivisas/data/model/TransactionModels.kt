package com.example.exchangededivisas.data.model

import java.time.LocalDateTime

sealed class Transaction(
    open val id: String,
    open val date: LocalDateTime,
    open val pair: String,
    open val unitPrice: Double
)

data class OrderOrOffer(
    override val id: String,
    override val date: LocalDateTime,
    override val pair: String,
    override val unitPrice: Double,
    val originalQuantity: Double,
    val originalTotal: Double,
    val executedQuantity: Double,
    val executedTotal: Double,
    val remainingQuantity: Double,
    val remainingTotal: Double,
    val type: TransactionType
) : Transaction(id, date, pair, unitPrice)

data class InstantTransaction(
    override val id: String,
    override val date: LocalDateTime,
    override val pair: String,
    override val unitPrice: Double,
    val quantity: Double,
    val total: Double,
    val type: TransactionType
) : Transaction(id, date, pair, unitPrice)

enum class TransactionType {
    BUY_ORDER, SELL_OFFER, INSTANT_BUY, INSTANT_SELL
}
