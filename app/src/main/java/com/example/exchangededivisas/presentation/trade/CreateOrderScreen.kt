package com.example.exchangededivisas.presentation.trade

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CreateOrderScreen(
    parMonedaId: Int = 1,
    viewModel: CreateOrderViewModel = viewModel()
) {
    var cantidad by remember { mutableStateOf("") }
    var precioUnitario by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    // Calcula el total en tiempo real
    val total = (cantidad.toDoubleOrNull() ?: 0.0) * (precioUnitario.toDoubleOrNull() ?: 0.0)

    // Validación simple
    val esValido = (cantidad.toDoubleOrNull() ?: 0.0) > 0.0 &&
            (precioUnitario.toDoubleOrNull() ?: 0.0) > 0.0

    // Limpia el formulario cuando la orden se crea con éxito
    if (uiState is CreateOrderUiState.Success) {
        LaunchedEffect(uiState) {
            cantidad = ""
            precioUnitario = ""
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Generar Orden de Compra",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Campo Cantidad
        Text(
            text = "Cantidad:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = cantidad,
            onValueChange = { cantidad = it },
            label = { Text("Ingresa la cantidad") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp),
            singleLine = true
        )

        // Campo Precio unitario
        Text(
            text = "Precio unitario (S/.):",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = precioUnitario,
            onValueChange = { precioUnitario = it },
            label = { Text("Precio unitario") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp),
            singleLine = true
        )

        // Total calculado
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            color = Color(0xFFE8F5E9),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Total: S/. %.2f".format(total),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                textAlign = TextAlign.Center
            )
        }

        // Mensaje de estado
        when (val state = uiState) {
            is CreateOrderUiState.Error -> Text(
                text = "❌ ${state.message}",
                color = Color.Red,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            is CreateOrderUiState.Success -> Text(
                text = "✅ Orden #${state.ordenId} creada con éxito",
                color = Color(0xFF4CAF50),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            else -> Spacer(modifier = Modifier.height(12.dp))
        }

        // Botón confirmar
        Button(
            onClick = {
                viewModel.crearOrdenCompra(
                    parMonedaId = parMonedaId,
                    cantidad = cantidad.toDouble(),
                    precioUnitario = precioUnitario.toDouble()
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = esValido && uiState !is CreateOrderUiState.Loading
        ) {
            if (uiState is CreateOrderUiState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Confirmar Orden",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}