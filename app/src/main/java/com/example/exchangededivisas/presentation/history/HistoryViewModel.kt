package com.example.exchangededivisas.presentation.history

import androidx.lifecycle.ViewModel
import com.example.exchangededivisas.data.model.InstantTransaction
import com.example.exchangededivisas.data.model.OrderOrOffer
import com.example.exchangededivisas.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime

data class HistoryState(
    val buyOrders: List<OrderOrOffer> = emptyList(),
    val sellOffers: List<OrderOrOffer> = emptyList(),
    val instantBuys: List<InstantTransaction> = emptyList(),
    val instantSells: List<InstantTransaction> = emptyList(),
    val filters: Map<TransactionType, DateFilter> = TransactionType.values().associateWith { DateFilter() },
    val pages: Map<TransactionType, Int> = TransactionType.values().associateWith { 0 },
    val isAscending: Map<TransactionType, Boolean> = TransactionType.values().associateWith { false }
)

data class DateFilter(
    val start: LocalDate? = null,
    val end: LocalDate? = null
)

class HistoryViewModel : ViewModel() {
    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val allBuyOrders = generateMockOrders(TransactionType.BUY_ORDER)
    private val allSellOffers = generateMockOrders(TransactionType.SELL_OFFER)
    private val allInstantBuys = generateMockInstant(TransactionType.INSTANT_BUY)
    private val allInstantSells = generateMockInstant(TransactionType.INSTANT_SELL)

    init {
        updateLists()
    }

    fun setDateFilter(type: TransactionType, start: LocalDate?, end: LocalDate?) {
        val newFilters = _state.value.filters.toMutableMap()
        newFilters[type] = DateFilter(start, end)
        _state.value = _state.value.copy(filters = newFilters, pages = _state.value.pages.toMutableMap().apply { put(type, 0) })
        updateLists()
    }

    fun toggleSort(type: TransactionType) {
        val newSort = _state.value.isAscending.toMutableMap()
        newSort[type] = !(newSort[type] ?: false)
        _state.value = _state.value.copy(isAscending = newSort, pages = _state.value.pages.toMutableMap().apply { put(type, 0) })
        updateLists()
    }

    fun changePage(type: TransactionType, delta: Int) {
        val newPages = _state.value.pages.toMutableMap()
        val currentPage = newPages[type] ?: 0
        newPages[type] = (currentPage + delta).coerceAtLeast(0)
        _state.value = _state.value.copy(pages = newPages)
        updateLists()
    }

    private fun updateLists() {
        _state.value = _state.value.copy(
            buyOrders = filterAndPaginate(allBuyOrders, TransactionType.BUY_ORDER),
            sellOffers = filterAndPaginate(allSellOffers, TransactionType.SELL_OFFER),
            instantBuys = filterAndPaginate(allInstantBuys, TransactionType.INSTANT_BUY),
            instantSells = filterAndPaginate(allInstantSells, TransactionType.INSTANT_SELL)
        )
    }

    private fun <T : com.example.exchangededivisas.data.model.Transaction> filterAndPaginate(
        list: List<T>,
        type: TransactionType
    ): List<T> {
        val filter = _state.value.filters[type]
        val ascending = _state.value.isAscending[type] ?: false
        val page = _state.value.pages[type] ?: 0

        return list.filter { item ->
            val date = item.date.toLocalDate()
            (filter?.start == null || !date.isBefore(filter.start)) &&
            (filter?.end == null || !date.isAfter(filter.end))
        }.sortedWith { a, b ->
            if (ascending) a.date.compareTo(b.date) else b.date.compareTo(a.date)
        }.drop(page * 5).take(5)
    }

    // Mock Data Generators
    private fun generateMockOrders(type: TransactionType) = List(15) { i ->
        OrderOrOffer(
            id = "O-$i",
            date = LocalDateTime.now().minusDays(i.toLong()).minusHours(i.toLong()),
            pair = "USD/PEN",
            unitPrice = 3.7 + (i * 0.01),
            originalQuantity = 100.0,
            originalTotal = 370.0 + (i * 1.0),
            executedQuantity = 40.0,
            executedTotal = 148.0,
            remainingQuantity = 60.0,
            remainingTotal = 222.0 + (i * 1.0),
            type = type
        )
    }

    private fun generateMockInstant(type: TransactionType) = List(15) { i ->
        InstantTransaction(
            id = "I-$i",
            date = LocalDateTime.now().minusDays(i.toLong()),
            pair = "EUR/USD",
            unitPrice = 1.08,
            quantity = 50.0,
            total = 54.0,
            type = type
        )
    }
}
