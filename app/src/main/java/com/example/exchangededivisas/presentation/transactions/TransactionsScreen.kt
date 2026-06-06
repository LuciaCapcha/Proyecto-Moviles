package com.example.exchangededivisas.presentation.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.exchangededivisas.data.model.OrderOrOffer
import com.example.exchangededivisas.data.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Órdenes de Compra", "Ofertas de Venta")
    val types = listOf(TransactionType.BUY_ORDER, TransactionType.SELL_OFFER)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Transacciones Activas",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Barra de pestañas similar a Historial
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentType = types[selectedTabIndex]
        val items = if (currentType == TransactionType.BUY_ORDER) state.buyOrders else state.sellOffers
        val filter = if (currentType == TransactionType.BUY_ORDER) state.buyFilter else state.sellFilter
        val page = if (currentType == TransactionType.BUY_ORDER) state.buyPage else state.sellPage
        val isAsc = if (currentType == TransactionType.BUY_ORDER) state.buySortAsc else state.sellSortAsc

        // Contenido de la pestaña seleccionada
        Column(modifier = Modifier.fillMaxSize()) {
            // Filtros de fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DatePickerButton("Desde", filter.start) { s -> 
                        viewModel.setDateFilter(currentType, s, filter.end) 
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    DatePickerButton("Hasta", filter.end) { e -> 
                        viewModel.setDateFilter(currentType, filter.start, e) 
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controles de Ordenamiento y Navegación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.toggleSort(currentType) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.DarkGray),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp))
                    Text(if (isAsc) " Más antiguos" else " Más recientes", fontSize = 11.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pág ${page + 1}", style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { viewModel.changePage(currentType, -1) }, enabled = page > 0) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { viewModel.changePage(currentType, 1) }, enabled = items.size == 5) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Siguiente", modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de transacciones
            if (items.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "No hay registros activos",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items) { item ->
                        ActiveTransactionCard(item) { viewModel.cancelTransaction(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveTransactionCard(item: OrderOrOffer, onCancel: (String) -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Cancelar operación") },
            text = { Text("¿Desea cancelar esta ${if (item.type == TransactionType.BUY_ORDER) "orden" else "oferta"} y reembolsar el saldo?") },
            confirmButton = {
                TextButton(onClick = { onCancel(item.id); showConfirm = false }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cerrar") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontSize = 12.sp, color = Color.Gray)
                Text(item.pair, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
            
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Precio Unitario", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("%.4f".format(item.unitPrice), fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Original", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("Cant: %.2f".format(item.originalQuantity), fontSize = 13.sp)
                    Text("Total: %.2f".format(item.originalTotal), fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Restante", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("Cant: %.2f".format(item.remainingQuantity), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Total: %.2f".format(item.remainingTotal), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F3F5), contentColor = Color.DarkGray)
            ) {
                Text("Cancelar Operación")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerButton(label: String, date: LocalDate?, onDateSelected: (LocalDate?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showDialog = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { onDateSelected(null); showDialog = false }) { Text("Limpiar") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(50.dp).clickable { showDialog = true },
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Column {
                Text(label, fontSize = 10.sp, color = Color.Gray)
                Text(date?.format(DateTimeFormatter.ofPattern("dd/MM/yy")) ?: "Cualquiera", fontSize = 12.sp)
            }
        }
    }
}
