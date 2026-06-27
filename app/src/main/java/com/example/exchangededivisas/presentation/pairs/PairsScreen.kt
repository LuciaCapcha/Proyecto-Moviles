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
import androidx.compose.material3.MenuAnchorType
import androidx.navigation.NavController
import com.example.exchangededivisas.data.repository.ExchangeRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairsScreen(navController: NavController) {

    var allPairs    by remember { mutableStateOf<List<ExchangeRepository.PairUi>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    var fromCurrency   by remember { mutableStateOf("Cualquiera") }
    var toCurrency     by remember { mutableStateOf("Cualquiera") }
    var expandedFrom   by remember { mutableStateOf(false) }
    var expandedTo     by remember { mutableStateOf(false) }
    var selectedButton by remember { mutableStateOf<String?>(null) }

    // Carga real de Supabase al iniciar
    LaunchedEffect(Unit) {
        isLoading = true
        ExchangeRepository.getAllPairs()
            .onSuccess { allPairs = it; errorMsg = null }
            .onFailure { errorMsg = it.message }
        isLoading = false
    }

    // Opciones de filtro basadas en los pares reales
    val currencyOptions: List<String> =
        listOf("Cualquiera") +
                allPairs.flatMap { listOf(it.fromCode, it.toCode) }.distinct().sorted()

    // Filtrado y ordenamiento
    val filteredPairs: List<ExchangeRepository.PairUi> = allPairs
        .filter { pair ->
            (fromCurrency == "Cualquiera" || pair.fromCode == fromCurrency) &&
                    (toCurrency   == "Cualquiera" || pair.toCode   == toCurrency)
        }
        .let { list ->
            when (selectedButton) {
                "Volumen" -> list.sortedByDescending { it.volume }
                "Compra"  -> list.sortedByDescending { it.bestBuy  ?: Double.MIN_VALUE }
                "Venta"   -> list.sortedByDescending { it.bestSell ?: Double.MIN_VALUE }
                "Margen"  -> list.sortedByDescending { it.margin   ?: Double.MIN_VALUE }
                else      -> list
            }
        }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text("Pares de Monedas", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        // Filtro: moneda a ofrecer
        ExposedDropdownMenuBox(expanded = expandedFrom, onExpandedChange = { expandedFrom = it }) {
            TextField(
                value = fromCurrency, onValueChange = {}, readOnly = true,
                label = { Text("Moneda a ofrecer") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrom) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
            )
            DropdownMenu(expanded = expandedFrom, onDismissRequest = { expandedFrom = false }) {
                currencyOptions.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = { fromCurrency = currency; expandedFrom = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filtro: moneda a recibir
        ExposedDropdownMenuBox(expanded = expandedTo, onExpandedChange = { expandedTo = it }) {
            TextField(
                value = toCurrency, onValueChange = {}, readOnly = true,
                label = { Text("Moneda a recibir") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTo) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
            )
            DropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                currencyOptions.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = { toCurrency = currency; expandedTo = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botones de ordenamiento (misma UI que antes)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Volumen", "Compra").forEach { label ->
                Button(
                    onClick = { selectedButton = label },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedButton == label) MaterialTheme.colorScheme.primary else Color.LightGray,
                        contentColor   = if (selectedButton == label) Color.White else Color.Black
                    )
                ) { Text(if (label == "Volumen") "Volumen \u2193" else label) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Venta", "Margen").forEach { label ->
                Button(
                    onClick = { selectedButton = label },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedButton == label) MaterialTheme.colorScheme.primary else Color.LightGray,
                        contentColor   = if (selectedButton == label) Color.White else Color.Black
                    )
                ) { Text(label) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            errorMsg != null -> Text("Error: $errorMsg", color = Color.Red)
            filteredPairs.isEmpty() -> Text("No hay pares disponibles.", color = Color.Gray)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredPairs) { pair: ExchangeRepository.PairUi ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("pairDetail/${pair.fromCode}_${pair.toCode}")
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${pair.fromCode} \u2192 ${pair.toCode}",
                                    style = MaterialTheme.typography.titleMedium)
                                Text("Volumen: ${"%.2f".format(pair.volume)}", color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Compra: ${pair.bestBuy?.let { "%.4f".format(it) } ?: "N/A"}")
                                Text("Venta: ${pair.bestSell?.let { "%.4f".format(it) } ?: "N/A"}")
                                Text("Margen: ${pair.margin?.let { "%.4f".format(it) } ?: "N/A"}")
                            }
                        }
                    }
                }
            }
        }
    }
}
