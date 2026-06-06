package com.example.exchangededivisas.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header (Ezchange + User info)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ezchange", 
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Bienvenida, Lucia",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HomeUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            is HomeUiState.Success -> {
                // 1. Par Más Activo
                HistoricalChartCard(
                    title = "Par Más Activo",
                    data = state.globalMostActive,
                    selectedRange = selectedRange,
                    onRangeSelected = { viewModel.setTimeRange(it) }
                )

                // 2. Auth Buttons or User Chart
                if (!state.isLoggedIn) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = onNavigateToLogin,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Iniciar sesión")
                            }
                            OutlinedButton(
                                onClick = onNavigateToRegister,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Registrarse")
                            }
                        }
                    }
                } else {
                    HistoricalChartCard(
                        title = "Tu Par Más Operado",
                        data = state.userMostActive,
                        selectedRange = selectedRange,
                        onRangeSelected = { viewModel.setTimeRange(it) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // Extra space for bottom nav
    }
}
