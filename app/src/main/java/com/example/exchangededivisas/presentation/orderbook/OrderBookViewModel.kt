package com.example.exchangededivisas.presentation.orderbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.api.OrderService
import com.example.exchangededivisas.models.OrderBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

// Los 3 posibles estados de la pantalla
sealed class OrderBookUiState {
    object Loading : OrderBookUiState()
    data class Success(val orderBook: OrderBook) : OrderBookUiState()
    data class Error(val message: String) : OrderBookUiState()
}

class OrderBookViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<OrderBookUiState>(OrderBookUiState.Loading)
    val uiState: StateFlow<OrderBookUiState> = _uiState

    private val orderService = ApiClient.getClient().create(OrderService::class.java)

    fun loadOrderBook(currencyPair: String) {
        viewModelScope.launch {
            _uiState.value = OrderBookUiState.Loading
            try {
                val response = orderService.getOrderBook(currencyPair).awaitResponse()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = OrderBookUiState.Success(response.body()!!)
                } else {
                    _uiState.value = OrderBookUiState.Error("Error del servidor: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = OrderBookUiState.Error("Sin conexión: ${e.message}")
            }
        }
    }
}

