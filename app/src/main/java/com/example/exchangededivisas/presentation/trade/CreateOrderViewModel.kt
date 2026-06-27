package com.example.exchangededivisas.presentation.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CreateOrderUiState {
    object Idle : CreateOrderUiState()
    object Loading : CreateOrderUiState()
    data class Success(val ordenId: Int) : CreateOrderUiState()
    data class Error(val message: String) : CreateOrderUiState()
}

class CreateOrderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CreateOrderUiState>(CreateOrderUiState.Idle)
    val uiState: StateFlow<CreateOrderUiState> = _uiState

    private val api = ApiClient.supabase

    fun crearOrdenCompra(
        parMonedaId: Int,
        cantidad: Double,
        precioUnitario: Double
    ) {
        viewModelScope.launch {
            _uiState.value = CreateOrderUiState.Loading
            try {
                val usuarioId = AppSession.currentUser.value.usuarioId
                val totalComprometido = cantidad * precioUnitario

                val body = mapOf(
                    "usuarioid" to usuarioId,
                    "parmonedaid" to parMonedaId,
                    "cantidadoriginal" to cantidad,
                    "cantidadobtenida" to 0.0,
                    "cantidadpendiente" to cantidad,
                    "preciounitario" to precioUnitario,
                    "totalcomprometido" to totalComprometido,
                    "totalejecutado" to 0.0,
                    "estado" to "Activa"
                )

                val resultado = api.insertOrdenCompra(body)
                val ordenCreada = resultado.firstOrNull()

                if (ordenCreada != null) {
                    _uiState.value = CreateOrderUiState.Success(ordenCreada.ordenCompraId)
                } else {
                    _uiState.value = CreateOrderUiState.Error("No se pudo crear la orden")
                }
            } catch (e: Exception) {
                _uiState.value = CreateOrderUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = CreateOrderUiState.Idle
    }
}