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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MenuAnchorType
import androidx.navigation.NavController
import com.example.exchangededivisas.data.repository.ExchangeRepository
import com.example.exchangededivisas.data.session.AppSession
import kotlin.math.ceil
import kotlin.math.max

// Colores que pide el criterio
private val ColorCompra = Color(0xFF2F80FF)  // azul
private val ColorVenta  = Color(0xFF22C55E)  // verde
private val ColorMargen = Color(0xFFFF9800)  // naranja

private const val PAGE_SIZE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairsScreen(navController: NavController) {

    var allPairs   by remember { mutableStateOf<List<ExchangeRepository.PairUi>>(emptyList()) }
    var recentIds  by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }

    var fromCurrency by remember { mutableStateOf("Cualquiera") }
    var toCurrency   by remember { mutableStateOf("Cualquiera") }
    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo   by remember { mutableStateOf(false) }

    var selectedSort by remember { mutableStateOf<String?>(null) } // null = orden por defecto
    var ascending    by remember { mutableStateOf(false) }
    var collapse     by remember { mutableStateOf(false) }
    var currentPage  by remember { mutableStateOf(0) }

    // Carga real de Supabase al iniciar
    LaunchedEffect(Unit) {
        isLoading = true
        val userId = AppSession.currentUser.value.usuarioId
        recentIds = ExchangeRepository.getRecentPairIds(userId)
        ExchangeRepository.getAllPairs()
            .onSuccess { allPairs = it; errorMsg = null }
            .onFailure { errorMsg = it.message }
        isLoading = false
    }

    // Si cambian filtros/orden/colapsar, volvemos a la página 1
    LaunchedEffect(fromCurrency, toCurrency, selectedSort, ascending, collapse) {
        currentPage = 0
    }

    val currencyOptions: List<String> =
        listOf("Cualquiera") +
                allPairs.flatMap { listOf(it.fromCode, it.toCode) }.distinct().sorted()

    // 1) Filtrar por moneda
    val filtered = allPairs.filter { pair ->
        (fromCurrency == "Cualquiera" || pair.fromCode == fromCurrency) &&
                (toCurrency == "Cualquiera" || pair.toCode == toCurrency)
    }

    // 2) Colapsar duplicados (deja 1 por par, moneda izquierda = primera alfabéticamente)
    val collapsed = if (collapse) {
        filtered.groupBy { setOf(it.fromCode, it.toCode) }
            .map { (_, group) -> group.minByOrNull { it.fromCode }!! }
    } else filtered

    // 3) Ordenar
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
                "Compra"  -> { p -> p.bestBuy ?: Double.NEGATIVE_INFINITY }
                "Venta"   -> { p -> p.bestSell ?: Double.POSITIVE_INFINITY }
                "Margen"  -> { p -> p.margin ?: Double.NEGATIVE_INFINITY }
                else      -> { p -> p.volume }
            }
            if (ascending) collapsed.sortedBy(selector) else collapsed.sortedByDescending(selector)
        }
    }

    // 4) Paginación (20 por página)
    val totalPages = max(1, ceil(sorted.size / PAGE_SIZE.toDouble()).toInt())
    val page = currentPage.coerceIn(0, totalPages - 1)
    val pageItems = sorted.drop(page * PAGE_SIZE).take(PAGE_SIZE)

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

        // Filtro: moneda a recibir + botón Colapsar duplicados
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExposedDropdownMenuBox(
                expanded = expandedTo,
                onExpandedChange = { expandedTo = it },
                modifier = Modifier.weight(1f)
            ) {
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Colapsar duplicados", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(8.dp))
            Switch(checked = collapse, onCheckedChange = { collapse = it })
            Spacer(Modifier.weight(1f))
            // Toggle ascendente / descendente
            TextButton(onClick = { ascending = !ascending }) {
                Text(if (ascending) "Ascendente \u2191" else "Descendente \u2193")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botones de ordenamiento
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
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { selectedSort = null }) { Text("Orden por defecto") }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            errorMsg != null -> Text("Error: $errorMsg", color = Color.Red)
            sorted.isEmpty() -> Text("No hay pares disponibles.", color = Color.Gray)
            else -> {
                Text(
                    "${sorted.size} pares \u2022 Página ${page + 1} de $totalPages",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(pageItems) { pair: ExchangeRepository.PairUi ->
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
                                    Text(
                                        "${pair.fromCode} \u2192 ${pair.toCode}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Vol: ${"%.2f".format(pair.volume)}", color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Compra: ${pair.bestBuy?.let { "%.4f".format(it) } ?: "N/A"}",
                                        color = ColorCompra, fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Venta: ${pair.bestSell?.let { "%.4f".format(it) } ?: "N/A"}",
                                        color = ColorVenta, fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Margen: ${pair.margin?.let { "%.4f".format(it) } ?: "N/A"}",
                                        color = ColorMargen, fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Controles de paginación
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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