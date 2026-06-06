package com.example.exchangededivisas.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangededivisas.data.model.CurrencyPairChartData
import com.example.exchangededivisas.data.model.HistoricalPrice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val globalMostActive: CurrencyPairChartData,
        val userMostActive: CurrencyPairChartData?,
        val isLoggedIn: Boolean
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
        loadDashboardData() // Reload data for the new range
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                // Mocking API call delay
                delay(1000)

                // Mocking logic: 
                // 1. Fetch global most active pair
                // 2. Check if user is logged in
                // 3. Fetch user's most active pair if logged in, else 2nd global
                
                val isLoggedIn = true // Simulated logged in for the screenshot look
                
                // Par más activo (empty as in screenshot)
                val globalPair = CurrencyPairChartData("USD", "PEN", emptyList())
                
                // Tu par más operado (with data as in screenshot)
                val userPair = getMockChartData("USD", "BRL", 24)

                _uiState.value = HomeUiState.Success(
                    globalMostActive = globalPair,
                    userMostActive = userPair,
                    isLoggedIn = isLoggedIn
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Error al cargar datos: ${e.message}")
            }
        }
    }

    // Helper to generate mock data
    private fun getMockChartData(base: String, quote: String, points: Int): CurrencyPairChartData {
        val prices = mutableListOf<HistoricalPrice>()
        var currentBuy = 3.70
        var currentSell = 3.75
        val now = LocalDateTime.now()
        
        for (i in points downTo 0) {
            currentBuy += (Math.random() - 0.5) * 0.05
            currentSell = currentBuy + 0.05 + (Math.random() * 0.02)
            prices.add(HistoricalPrice(now.minusHours(i.toLong()), currentBuy, currentSell))
        }
        
        return CurrencyPairChartData(base, quote, prices)
    }
}
