package com.example.exchangededivisas.presentation.pairs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.session.AppSession
import kotlin.math.ceil
import kotlin.math.max

private val ColorCompra = Color(0xFF2F80FF)
private val ColorVenta = Color(0xFF22C55E)
private val ColorMargen = Color(0xFFFF9800)
private const val PAGE_SIZE = 20

@Composable
fun PairsScreen(navController: NavController) {
    var allPairs by remember { mutableStateOf<List<ExchangeRepository.PairUi>>(emptyList()) }
    var recentIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var fromCurrency by remember { mutableStateOf("Cualquiera") }
    var toCurrency by remember { mutableStateOf("Cualquiera") }
    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }

    var selectedSort by remember { mutableStateOf<String?>(null) }
    var ascending by remember { mutableStateOf(false) }
    var collapse by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }

    suspend fun reloadPairs() {
        isLoading = true
        errorMsg = null
        val userId = AppSession.currentUser.value.usuarioId
        recentIds = ExchangeRepository.getRecentPairIds(userId)
        ExchangeRepository.getAllPairs()
            .onSuccess { allPairs = it }
            .onFailure { errorMsg = it.message ?: "No se pudieron cargar los pares." }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        reloadPairs()
    }

    LaunchedEffect(fromCurrency, toCurrency, selectedSort, ascending, collapse) {
        currentPage = 0
    }

    val currencyOptions = listOf("Cualquiera") +
            allPairs.flatMap { listOf(it.fromCode, it.toCode) }.distinct().sorted()

    val filtered = allPairs.filter { pair ->
        (fromCurrency == "Cualquiera" || pair.fromCode == fromCurrency) &&
                (toCurrency == "Cualquiera" || pair.toCode == toCurrency)
    }

    val collapsed = if (collapse) {
        filtered.groupBy { setOf(it.fromCode, it.toCode) }
            .map { (_, group) -> group.minByOrNull { it.fromCode }!! }
    } else {
        filtered
    }

    fun recentRank(p: ExchangeRepository.PairUi): Int {
        val idx = recentIds.indexOf(p.parMonedaId)
        return if (idx >= 0) idx else Int.MAX_VALUE
    }

    val sorted = when (selectedSort) {
        null -> collapsed.sortedWith(
            compareBy<ExchangeRepository.PairUi> { recentRank(it) }
                .thenByDescending { it.volume }
        )
        else -> {
            val selector: (ExchangeRepository.PairUi) -> Double = when (selectedSort) {
                "Volumen" -> { p -> p.volume }
                "Compra" -> { p -> p.bestBuy ?: Double.NEGATIVE_INFINITY }
                "Venta" -> { p -> p.bestSell ?: Double.POSITIVE_INFINITY }
                "Margen" -> { p -> p.margin ?: Double.NEGATIVE_INFINITY }
                else -> { p -> p.volume }
            }
            if (ascending) collapsed.sortedBy(selector) else collapsed.sortedByDescending(selector)
        }
    }

    val totalPages = max(1, ceil(sorted.size / PAGE_SIZE.toDouble()).toInt())
    val page = currentPage.coerceIn(0, totalPages - 1)
    val pageItems = sorted.drop(page * PAGE_SIZE).take(PAGE_SIZE)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Pares de Monedas", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        CurrencySelector(
            label = "Moneda a ofrecer",
            value = fromCurrency,
            options = currencyOptions.filter { it == "Cualquiera" || it != toCurrency },
            expanded = expandedFrom,
            onExpandedChange = { expandedFrom = it },
            onSelected = {
                fromCurrency = it
                if (toCurrency == it && it != "Cualquiera") toCurrency = "Cualquiera"
                expandedFrom = false
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        CurrencySelector(
            label = "Moneda a recibir",
            value = toCurrency,
            options = currencyOptions.filter { it == "Cualquiera" || it != fromCurrency },
            expanded = expandedTo,
            onExpandedChange = { expandedTo = it },
            onSelected = {
                toCurrency = it
                if (fromCurrency == it && it != "Cualquiera") fromCurrency = "Cualquiera"
                expandedTo = false
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Colapsar duplicados", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Switch(checked = collapse, onCheckedChange = { collapse = it })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { ascending = !ascending }) {
                Text(if (ascending) "Ascendente ↑" else "Descendente ↓")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { selectedSort = null }) {
                Text("Orden por defecto")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Volumen", "Compra").forEach { label ->
                SortButton(label, selectedSort == label, Modifier.weight(1f)) { selectedSort = label }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Venta", "Margen").forEach { label ->
                SortButton(label, selectedSort == label, Modifier.weight(1f)) { selectedSort = label }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            errorMsg != null -> {
                Text("Error: $errorMsg", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { /* reload by recomposition */ }) { Text("Intente volver a entrar") }
            }
            sorted.isEmpty() -> Text("No hay pares disponibles.", color = Color.Gray)
            else -> {
                Text(
                    "${sorted.size} pares • Página ${page + 1} de $totalPages",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                pageItems.forEach { pair ->
                    PairCard(pair = pair, navController = navController)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { if (page > 0) currentPage = page - 1 }, enabled = page > 0) {
                        Text("Anterior")
                    }
                    Text("${page + 1} / $totalPages")
                    Button(
                        onClick = { if (page < totalPages - 1) currentPage = page + 1 },
                        enabled = page < totalPages - 1
                    ) {
                        Text("Siguiente")
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySelector(
    label: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (String) -> Unit
) {
    Text(label, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(value)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp)
        ) {
            options.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = { onSelected(currency) }
                )
            }
        }
    }
}

@Composable
private fun PairCard(pair: ExchangeRepository.PairUi, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("pairDetail/${pair.fromCode}_${pair.toCode}") }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "${pair.fromCode} → ${pair.toCode}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Vol: ${"%.2f".format(pair.volume)} ${pair.toCode}", color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Compra: ${pair.bestBuy?.let { "%.4f".format(it) } ?: "N/A"}",
                    color = ColorCompra,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Venta: ${pair.bestSell?.let { "%.4f".format(it) } ?: "N/A"}",
                    color = ColorVenta,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Margen: ${pair.margin?.let { "%.4f".format(it) } ?: "N/A"}",
                    color = ColorMargen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SortButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
            contentColor = if (selected) Color.White else Color.Black
        )
    ) { Text(label) }
}
