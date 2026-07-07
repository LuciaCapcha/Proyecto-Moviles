package com.example.exchangededivisas.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangededivisas.data.model.CurrencyPairChartData
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val globalMostActive: CurrencyPairChartData?,
        val userMostActive: CurrencyPairChartData?,
        val isLoggedIn: Boolean,
        val userName: String,
        val isLoadingCharts: Boolean = false,
        val isRestricted: Boolean = false,
        val restrictionReason: String? = null
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedRange = MutableStateFlow("1d")
    val selectedRange: StateFlow<String> = _selectedRange.asStateFlow()

    init {
        loadDashboardData()
    }

    fun setTimeRange(range: String) {
        _selectedRange.value = range
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            val currentUser = AppSession.currentUser.value
            val isLoggedIn = currentUser.usuarioId != 0

            // Emitir un estado de éxito parcial inmediatamente para mostrar el saludo
            if (_uiState.value !is HomeUiState.Success) {
                _uiState.value = HomeUiState.Success(
                    globalMostActive = null,
                    userMostActive = null,
                    isLoggedIn = isLoggedIn,
                    userName = currentUser.nombreUsuario,
                    isLoadingCharts = true,
                    isRestricted = currentUser.estado.equals("Restringido", ignoreCase = true)
                )
            }

            try {
                val homeData = ExchangeRepository.loadHomeData(currentUser.usuarioId)

                _uiState.value = HomeUiState.Success(
                    globalMostActive = homeData.globalMostActive,
                    userMostActive = homeData.userMostActive,
                    isLoggedIn = isLoggedIn,
                    userName = currentUser.nombreUsuario,
                    isLoadingCharts = false,
                    isRestricted = homeData.isRestricted,
                    restrictionReason = homeData.restrictionReason
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    "Error al cargar datos: ${e.message}"
                )
            }
        }
    }
}
