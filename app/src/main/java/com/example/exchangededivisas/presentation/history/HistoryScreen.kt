package com.example.exchangededivisas.presentation.history

import androidx.compose.foundation.background
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
import com.example.exchangededivisas.data.model.InstantTransaction
import com.example.exchangededivisas.data.model.OrderOrOffer
import com.example.exchangededivisas.data.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Órd. Compra", "Of. Venta", "Comp. Inmediata", "Vent. Inmediata")
    val types = listOf(
        TransactionType.BUY_ORDER, 
        TransactionType.SELL_OFFER, 
        TransactionType.INSTANT_BUY, 
        TransactionType.INSTANT_SELL
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Historial de Transacciones",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp,
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
        TransactionListColumn(
            type = currentType,
            state = state,
            onDateChange = { start, end -> viewModel.setDateFilter(currentType, start, end) },
            onSortToggle = { viewModel.toggleSort(currentType) },
            onPageChange = { delta -> viewModel.changePage(currentType, delta) }
        )
    }
}

@Composable
fun TransactionListColumn(
    type: TransactionType,
    state: HistoryState,
    onDateChange: (LocalDate?, LocalDate?) -> Unit,
    onSortToggle: () -> Unit,
    onPageChange: (Int) -> Unit
) {
    val filter = state.filters[type]
    val items = when(type) {
        TransactionType.BUY_ORDER -> state.buyOrders
        TransactionType.SELL_OFFER -> state.sellOffers
        TransactionType.INSTANT_BUY -> state.instantBuys
        TransactionType.INSTANT_SELL -> state.instantSells
    }
    val page = state.pages[type] ?: 0
    val ascending = state.isAscending[type] ?: false

    Column(modifier = Modifier.fillMaxSize()) {
        // Date Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.weight(1f)) {
                DatePickerField("Desde", filter?.start) { onDateChange(it, filter?.end) }
            }
            Box(Modifier.weight(1f)) {
                DatePickerField("Hasta", filter?.end) { onDateChange(filter?.start, it) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sorting & Navigation Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSortToggle) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Invertir orden",
                    tint = if (ascending) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pág ${page + 1}", style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { onPageChange(-1) }, enabled = page > 0) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Anterior")
                }
                IconButton(onClick = { onPageChange(1) }, enabled = items.size == 5) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Siguiente")
                }
            }
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin registros para este periodo")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { tx ->
                    if (tx is OrderOrOffer) {
                        OrderOfferCard(tx)
                    } else if (tx is InstantTransaction) {
                        InstantTxCard(tx)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderOfferCard(tx: OrderOrOffer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(tx.pair, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            
            TransactionDetailRow("Precio Unit.", "%.4f".format(tx.unitPrice))
            
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Original", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("Cant: %.2f".format(tx.originalQuantity), fontSize = 12.sp)
                    Text("Total: %.2f".format(tx.originalTotal), fontSize = 12.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Operado", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("Cant: %.2f".format(tx.executedQuantity), fontSize = 12.sp)
                    Text("Total: %.2f".format(tx.executedTotal), fontSize = 12.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("Restante", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("Cant: %.2f".format(tx.remainingQuantity), fontSize = 12.sp)
                    Text("Total: %.2f".format(tx.remainingTotal), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun InstantTxCard(tx: InstantTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tx.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(tx.pair, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            TransactionDetailRow("Precio Unit.", "%.4f".format(tx.unitPrice))
            TransactionDetailRow("Cantidad", "%.2f".format(tx.quantity))
            TransactionDetailRow("Total", "%.2f".format(tx.total))
        }
    }
}

@Composable
fun TransactionDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(label: String, date: LocalDate?, onDateSelected: (LocalDate?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val selectedDate = Instant.ofEpochMilli(selectedMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(selectedDate)
                    }
                    showDialog = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    onDateSelected(null)
                    showDialog = false 
                }) { Text("Limpiar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Surface(
        modifier = Modifier
            .height(50.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    text = date?.format(DateTimeFormatter.ofPattern("dd/MM/yy")) ?: "Cualquiera",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
