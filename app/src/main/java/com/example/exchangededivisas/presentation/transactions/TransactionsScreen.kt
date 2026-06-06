package com.example.exchangededivisas.presentation.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ActiveOrder(
    val id: Int,
    val type: String,
    val pair: String,
    val unitPrice: Double,
    val originalAmount: Double,
    val remainingAmount: Double,
    val remainingTotal: Double
)

@Composable
fun TransactionsScreen() {

    var walletBalance by remember {
        mutableDoubleStateOf(2800.0)
    }

    var orders by remember {
        mutableStateOf(
            mutableListOf(
                ActiveOrder(
                    1,
                    "Orden de compra",
                    "PEN → USD",
                    3.72,
                    500.0,
                    300.0,
                    1116.0
                ),
                ActiveOrder(
                    2,
                    "Oferta de venta",
                    "USD → PEN",
                    3.80,
                    400.0,
                    250.0,
                    950.0
                ),
                ActiveOrder(
                    3,
                    "Orden de compra",
                    "PEN → EUR",
                    4.12,
                    200.0,
                    200.0,
                    824.0
                )
            )
        )
    }

    var selectedOrder by remember {
        mutableStateOf<ActiveOrder?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Transacciones Activas",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Saldo disponible: %.2f PEN".format(walletBalance),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(orders, key = { it.id }) { order ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = order.type,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Par: ${order.pair}")
                        Text("Precio unitario: ${order.unitPrice}")
                        Text("Cantidad original: ${order.originalAmount}")
                        Text("Cantidad restante: ${order.remainingAmount}")
                        Text("Total restante: ${order.remainingTotal}")

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {

                            OutlinedButton(
                                onClick = {
                                    selectedOrder = order
                                }
                            ) {
                                Text("Cancelar")
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedOrder != null) {

        AlertDialog(
            onDismissRequest = {
                selectedOrder = null
            },
            title = {
                Text("Cancelar operación")
            },
            text = {
                Text(
                    "¿Desea cancelar esta orden/oferta activa y reembolsar el saldo comprometido?"
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        walletBalance += selectedOrder!!.remainingTotal

                        orders = orders
                            .filter { it.id != selectedOrder!!.id }
                            .toMutableList()

                        selectedOrder = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedOrder = null
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}