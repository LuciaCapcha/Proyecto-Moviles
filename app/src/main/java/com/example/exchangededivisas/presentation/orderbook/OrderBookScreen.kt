package com.example.exchangededivisas.presentation.orderbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.exchangededivisas.models.OrderItem

@Composable
fun OrderBookScreen(
    currencyPair: String = "USD/PEN",
    viewModel: OrderBookViewModel = viewModel()
) {
    // Cuando la pantalla abre, le dice al ViewModel que cargue los datos
    LaunchedEffect(Unit) {
        viewModel.loadOrderBook(1) // 1 = par USD/PEN, ajusta según tu BD
    }

    // Escucha el estado actual del ViewModel (Loading, Success o Error)
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Libro de Órdenes",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = currencyPair,
            fontSize = 18.sp,
            color = Color(0xFF2196F3),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        // Según el estado, muestra una cosa u otra
        when (val state = uiState) {

            is OrderBookUiState.Loading -> {
                // Muestra un círculo girando mientras carga
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is OrderBookUiState.Error -> {
                // Muestra el mensaje de error en rojo
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Color.Red, textAlign = TextAlign.Center)
                }
            }

            is OrderBookUiState.Success -> {
                val orderBook = state.orderBook

                // Calculamos el valor máximo de cantidad para dibujar las barras proporcionales
                val allQuantities = (orderBook.buyOrders + orderBook.sellOffers).map { it.quantity }
                val maxQty = allQuantities.maxOrNull() ?: 1.0

                // Encabezados: VENTA | COMPRA
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "VENTA",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(1.dp))
                    Text(
                        text = "COMPRA",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Sub-encabezados: Cant. / Precio
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cant.", fontSize = 11.sp, color = Color.Gray)
                        Text("Precio", fontSize = 11.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(1.dp))
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Precio", fontSize = 11.sp, color = Color.Gray)
                        Text("Cant.", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Las dos columnas de órdenes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // --- COLUMNA IZQUIERDA: Ofertas de Venta (rojo) ---
                    val sellOffers = orderBook.sellOffers
                        .sortedBy { it.unitPrice }
                        .take(10)

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (sellOffers.isEmpty()) {
                            item {
                                Text(
                                    "Sin ofertas",
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            items(sellOffers) { order ->
                                OrderRow(order = order, maxQty = maxQty, isBuy = false)
                            }
                        }
                    }

                    // --- SEPARADOR VERTICAL (esto es lo que reemplaza al Divider roto) ---
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color.LightGray)
                    )

                    // --- COLUMNA DERECHA: Órdenes de Compra (verde) ---
                    val buyOrders = orderBook.buyOrders
                        .sortedByDescending { it.unitPrice }
                        .take(10)

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (buyOrders.isEmpty()) {
                            item {
                                Text(
                                    "Sin órdenes",
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            items(buyOrders) { order ->
                                OrderRow(order = order, maxQty = maxQty, isBuy = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Esta función dibuja UNA fila de orden con barra de volumen de fondo
@Composable
fun OrderRow(order: OrderItem, maxQty: Double, isBuy: Boolean) {
    val barColor = if (isBuy) Color(0x224CAF50) else Color(0x22F44336)
    val textColor = if (isBuy) Color(0xFF4CAF50) else Color(0xFFF44336)
    val fillFraction = (order.quantity / maxQty).toFloat().coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        // Barra de fondo que representa el volumen
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fillFraction)
                .background(barColor)
                .align(if (isBuy) Alignment.CenterStart else Alignment.CenterEnd)
        )

        // Texto encima de la barra
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBuy) {
                Text("S/. ${"%.4f".format(order.unitPrice)}", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Bold)
                Text("${"%.2f".format(order.quantity)}", fontSize = 12.sp)
            } else {
                Text("${"%.2f".format(order.quantity)}", fontSize = 12.sp)
                Text("S/. ${"%.4f".format(order.unitPrice)}", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

