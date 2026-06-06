package com.example.exchangededivisas.presentation.transactions

import androidx.lifecycle.ViewModel
import com.example.exchangededivisas.data.model.OrderOrOffer
import com.example.exchangededivisas.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime

data class TransactionsState(
    val buyOrders: List<OrderOrOffer> = emptyList(),
    val sellOffers: List<OrderOrOffer> = emptyList(),
    val buyFilter: DateRange = DateRange(),
    val sellFilter: DateRange = DateRange(),
    val buyPage: Int = 0,
    val sellPage: Int = 0,
    val buySortAsc: Boolean = false,
    val sellSortAsc: Boolean = false
)

data class DateRange(val start: LocalDate? = null, val end: LocalDate? = null)

class TransactionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TransactionsState())
    val uiState: StateFlow<TransactionsState> = _uiState.asStateFlow()

    // Mock data storage
    private var allBuyOrders = generateMockData(TransactionType.BUY_ORDER)
    private var allSellOffers = generateMockData(TransactionType.SELL_OFFER)

    init {
        refreshLists()
    }

    fun setDateFilter(type: TransactionType, start: LocalDate?, end: LocalDate?) {
        if (type == TransactionType.BUY_ORDER) {
            _uiState.value = _uiState.value.copy(buyFilter = DateRange(start, end), buyPage = 0)
        } else {
            _uiState.value = _uiState.value.copy(sellFilter = DateRange(start, end), sellPage = 0)
        }
        refreshLists()
    }

    fun toggleSort(type: TransactionType) {
        if (type == TransactionType.BUY_ORDER) {
            _uiState.value = _uiState.value.copy(buySortAsc = !_uiState.value.buySortAsc, buyPage = 0)
        } else {
            _uiState.value = _uiState.value.copy(sellSortAsc = !_uiState.value.sellSortAsc, sellPage = 0)
        }
        refreshLists()
    }

    fun changePage(type: TransactionType, delta: Int) {
        if (type == TransactionType.BUY_ORDER) {
            val next = (_uiState.value.buyPage + delta).coerceAtLeast(0)
            _uiState.value = _uiState.value.copy(buyPage = next)
        } else {
            val next = (_uiState.value.sellPage + delta).coerceAtLeast(0)
            _uiState.value = _uiState.value.copy(sellPage = next)
        }
        refreshLists()
    }

    fun cancelTransaction(id: String) {
        allBuyOrders = allBuyOrders.filter { it.id != id }
        allSellOffers = allSellOffers.filter { it.id != id }
        refreshLists()
    }

    private fun refreshLists() {
        val current = _uiState.value
        _uiState.value = current.copy(
            buyOrders = filterSortPaginate(allBuyOrders, current.buyFilter, current.buySortAsc, current.buyPage),
            sellOffers = filterSortPaginate(allSellOffers, current.sellFilter, current.sellSortAsc, current.sellPage)
        )
    }

    private fun filterSortPaginate(list: List<OrderOrOffer>, range: DateRange, asc: Boolean, page: Int): List<OrderOrOffer> {
        return list.filter { item ->
            val d = item.date.toLocalDate()
            (range.start == null || !d.isBefore(range.start)) && (range.end == null || !d.isAfter(range.end))
        }.sortedWith { a, b ->
            if (asc) a.date.compareTo(b.date) else b.date.compareTo(a.date)
        }.drop(page * 5).take(5)
    }

    private fun generateMockData(type: TransactionType) = List(12) { i ->
        OrderOrOffer(
            id = "${type.name}-$i",
            date = LocalDateTime.now().minusDays(i.toLong()),
            pair = if (type == TransactionType.BUY_ORDER) "PEN/USD" else "USD/PEN",
            unitPrice = 3.75,
            originalQuantity = 100.0,
            originalTotal = 375.0,
            executedQuantity = 20.0,
            executedTotal = 75.0,
            remainingQuantity = 80.0,
            remainingTotal = 300.0,
            type = type
        )
    }
}
