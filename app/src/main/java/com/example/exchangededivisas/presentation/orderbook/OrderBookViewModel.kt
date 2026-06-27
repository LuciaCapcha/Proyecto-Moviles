package com.example.exchangededivisas.presentation.orderbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.data.remote.OfertaVentaDto
import com.example.exchangededivisas.data.remote.OrdenCompraDto
import com.example.exchangededivisas.models.OrderBook
import com.example.exchangededivisas.models.OrderItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class OrderBookUiState {
    object Loading : OrderBookUiState()
    data class Success(val orderBook: OrderBook) : OrderBookUiState()
    data class Error(val message: String) : OrderBookUiState()
}

class OrderBookViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<OrderBookUiState>(OrderBookUiState.Loading)
    val uiState: StateFlow<OrderBookUiState> = _uiState

    private val api = ApiClient.supabase

    fun loadOrderBook(parMonedaId: Int) {
        viewModelScope.launch {
            _uiState.value = OrderBookUiState.Loading
            try {
                // Trae ofertas de venta y órdenes de compra en paralelo
                val ventas = api.getOfertasVentaByPair(
                    parMonedaId = "eq.$parMonedaId",
                    order = "preciounitario.asc,fechacreacion.asc"
                )
                val compras = api.getOrdenesCompraByPair(
                    parMonedaId = "eq.$parMonedaId",
                    order = "preciounitario.desc,fechacreacion.asc"
                )

                val orderBook = OrderBook(
                    currencyPair = "Par $parMonedaId",
                    buyOrders = compras.map { it.toOrderItem() },
                    sellOffers = ventas.map { it.toOrderItem() }
                )
                _uiState.value = OrderBookUiState.Success(orderBook)
            } catch (e: Exception) {
                _uiState.value = OrderBookUiState.Error("Error: ${e.message}")
            }
        }
    }

    // Convierte OrdenCompraDto → OrderItem (el modelo que ya usa tu pantalla)
    private fun OrdenCompraDto.toOrderItem() = OrderItem(
        orderId = ordenCompraId.toString(),
        userId = usuarioId.toString(),
        quantity = cantidadPendiente,
        unitPrice = precioUnitario
    )

    // Convierte OfertaVentaDto → OrderItem
    private fun OfertaVentaDto.toOrderItem() = OrderItem(
        orderId = ofertaVentaId.toString(),
        userId = usuarioId.toString(),
        quantity = cantidadPendiente,
        unitPrice = precioUnitario
    )
}