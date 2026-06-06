package com.example.exchangededivisas.presentation.pairs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class CurrencyPair(
    val from: String,
    val to: String,
    val volume: Double,
    val buy: Double?,
    val sell: Double?,
    val margin: Double?
)

val mockPairs = listOf(
    CurrencyPair("PEN", "USD", 2000.0, 3.72, 3.68, 0.04),
    CurrencyPair("PEN", "EUR", 1800.0, 0.25, 0.24, 0.01),
    CurrencyPair("PEN", "GBP", 1500.0, 0.21, 0.20, 0.01),
    CurrencyPair("USD", "BRL", 1500.0, 5.10, 5.05, 0.05),
    CurrencyPair("EUR", "GBP", 800.0, 1.12, 1.15, 0.03),
    CurrencyPair("GBP", "USD", 1200.0, 1.30, 1.28, 0.02),
    CurrencyPair("JPY", "USD", 5000.0, 0.0091, 0.0090, 0.0001),
    CurrencyPair("CAD", "USD", 950.0, 0.75, 0.74, 0.01),
    CurrencyPair("MXN", "USD", 3000.0, 0.055, 0.054, 0.001),
    CurrencyPair("CLP", "USD", 2200.0, 0.0012, 0.0011, 0.0001),
    CurrencyPair("ARS", "USD", 1800.0, 0.0025, 0.0024, 0.0001),
    CurrencyPair("COP", "USD", 2700.0, 0.00025, 0.00024, 0.00001)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairsScreen(navController: NavController) {
    var fromCurrency by remember { mutableStateOf("Cualquiera") }
    var toCurrency by remember { mutableStateOf("Cualquiera") }
    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }
    var collapseDuplicates by remember { mutableStateOf(false) }


    var selectedButton by remember { mutableStateOf<String?>(null) }

    val currencies = listOf("PEN", "USD", "EUR", "GBP", "JPY", "CAD", "MXN", "CLP", "ARS", "COP")


    var filteredPairs = mockPairs.filter { pair ->
        (fromCurrency == "Cualquiera" || pair.from == fromCurrency) &&
                (toCurrency == "Cualquiera" || pair.to == toCurrency)
    }.let { list ->
        if (collapseDuplicates) list.distinctBy { it.from to it.to } else list
    }

    filteredPairs = when (selectedButton) {
        "Volumen" -> filteredPairs.sortedByDescending { it.volume }
        "Compra" -> filteredPairs.sortedByDescending { it.buy ?: Double.MIN_VALUE }
        "Venta" -> filteredPairs.sortedByDescending { it.sell ?: Double.MIN_VALUE }
        "Margen" -> filteredPairs.sortedByDescending { it.margin ?: Double.MIN_VALUE }
        else -> filteredPairs
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pares de Monedas", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = expandedFrom,
            onExpandedChange = { expandedFrom = it }
        ) {
            TextField(
                value = fromCurrency,
                onValueChange = {},
                readOnly = true,
                label = { Text("Moneda a ofrecer") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrom) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            DropdownMenu(expanded = expandedFrom, onDismissRequest = { expandedFrom = false }) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            fromCurrency = currency
                            expandedFrom = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filtro: moneda a recibir
        ExposedDropdownMenuBox(
            expanded = expandedTo,
            onExpandedChange = { expandedTo = it }
        ) {
            TextField(
                value = toCurrency,
                onValueChange = {},
                readOnly = true,
                label = { Text("Moneda a recibir") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTo) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            DropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            toCurrency = currency
                            expandedTo = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = collapseDuplicates, onCheckedChange = { collapseDuplicates = it })
            Text("Colapsar duplicados")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { selectedButton = "Volumen" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedButton == "Volumen") MaterialTheme.colorScheme.primary else Color.LightGray,
                    contentColor = if (selectedButton == "Volumen") Color.White else Color.Black
                )
            ) { Text("Volumen ↓") }

            Button(
                onClick = { selectedButton = "Compra" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedButton == "Compra") MaterialTheme.colorScheme.primary else Color.LightGray,
                    contentColor = if (selectedButton == "Compra") Color.White else Color.Black
                )
            ) { Text("Compra") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { selectedButton = "Venta" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedButton == "Venta") MaterialTheme.colorScheme.primary else Color.LightGray,
                    contentColor = if (selectedButton == "Venta") Color.White else Color.Black
                )
            ) { Text("Venta") }

            Button(
                onClick = { selectedButton = "Margen" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedButton == "Margen") MaterialTheme.colorScheme.primary else Color.LightGray,
                    contentColor = if (selectedButton == "Margen") Color.White else Color.Black
                )
            ) { Text("Margen") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Lista desplazable de pares filtrados y ordenados
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredPairs) { pair ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("pairDetail/${pair.from}_${pair.to}")
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${pair.from} → ${pair.to}", style = MaterialTheme.typography.titleMedium)
                            Text("Volumen: ${pair.volume}")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Compra: ${pair.buy ?: "N/A"}")
                            Text("Venta: ${pair.sell ?: "N/A"}")
                            Text("Margen: ${pair.margin ?: "N/A"}")
                        }
                    }
                }
            }
        }
    }
}
