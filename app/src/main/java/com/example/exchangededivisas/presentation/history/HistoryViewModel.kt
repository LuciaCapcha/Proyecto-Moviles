package com.example.exchangededivisas.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangededivisas.data.model.InstantTransaction
import com.example.exchangededivisas.data.model.OrderOrOffer
import com.example.exchangededivisas.data.model.TransactionType
import com.example.exchangededivisas.data.repository.ExchangeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HistoryState(
    val buyOrders: List<OrderOrOffer> = emptyList(),
    val sellOffers: List<OrderOrOffer> = emptyList(),
    val instantBuys: List<InstantTransaction> = emptyList(),
    val instantSells: List<InstantTransaction> = emptyList(),
    val filters: Map<TransactionType, DateFilter> = TransactionType.values().associateWith { DateFilter() },
    val pages: Map<TransactionType, Int> = TransactionType.values().associateWith { 0 },
    val isAscending: Map<TransactionType, Boolean> = TransactionType.values().associateWith { false },
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class DateFilter(
    val start: LocalDate? = null,
    val end: LocalDate? = null
)

class HistoryViewModel : ViewModel() {
    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private var allBuyOrders: List<OrderOrOffer> = emptyList()
    private var allSellOffers: List<OrderOrOffer> = emptyList()
    private var allInstantBuys: List<InstantTransaction> = emptyList()
    private var allInstantSells: List<InstantTransaction> = emptyList()

    fun load(usuarioId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            ExchangeRepository.loadTransactionHistory(usuarioId)
                .onSuccess { data ->
                    allBuyOrders = data.buyOrders
                    allSellOffers = data.sellOffers
                    allInstantBuys = data.instantBuys
                    allInstantSells = data.instantSells
                    updateLists(isLoading = false, errorMessage = null)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo cargar el historial."
                    )
                }
        }
    }

    fun setDateFilter(type: TransactionType, start: LocalDate?, end: LocalDate?) {
        val newFilters = _state.value.filters.toMutableMap()
        newFilters[type] = DateFilter(start, end)
        _state.value = _state.value.copy(
            filters = newFilters,
            pages = _state.value.pages.toMutableMap().apply { put(type, 0) }
        )
        updateLists()
    }

    fun toggleSort(type: TransactionType) {
        val newSort = _state.value.isAscending.toMutableMap()
        newSort[type] = !(newSort[type] ?: false)
        _state.value = _state.value.copy(
            isAscending = newSort,
            pages = _state.value.pages.toMutableMap().apply { put(type, 0) }
        )
        updateLists()
    }

    fun changePage(type: TransactionType, delta: Int) {
        val newPages = _state.value.pages.toMutableMap()
        val currentPage = newPages[type] ?: 0
        newPages[type] = (currentPage + delta).coerceAtLeast(0)
        _state.value = _state.value.copy(pages = newPages)
        updateLists()
    }

    private fun updateLists(
        isLoading: Boolean = _state.value.isLoading,
        errorMessage: String? = _state.value.errorMessage
    ) {
        _state.value = _state.value.copy(
            buyOrders = filterAndPaginate(allBuyOrders, TransactionType.BUY_ORDER),
            sellOffers = filterAndPaginate(allSellOffers, TransactionType.SELL_OFFER),
            instantBuys = filterAndPaginate(allInstantBuys, TransactionType.INSTANT_BUY),
            instantSells = filterAndPaginate(allInstantSells, TransactionType.INSTANT_SELL),
            isLoading = isLoading,
            errorMessage = errorMessage
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
}
