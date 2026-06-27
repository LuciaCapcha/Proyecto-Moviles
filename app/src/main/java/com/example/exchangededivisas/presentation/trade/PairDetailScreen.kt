package com.example.exchangededivisas.presentation.trade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun PairDetailScreen(navController: NavController) {
    // Simulación de usuario con sesión iniciada (Criterio de aceptación)
    val usuarioInicioSesion by remember { mutableStateOf(true) }

    var rangoTiempoSeleccionado by remember { mutableStateOf("Último día") }
    val rangosTiempo = listOf("Último día", "Última semana", "Último mes", "Último año", "Tiempo total")
    var menuAccionesDesplegado by remember { mutableStateOf(false) }
    var puntoInteractuado by remember { mutableStateOf<Int?>(null) }

    val preciosCompra = listOf(3.72, 3.74, 3.71, 3.75, 3.73, 3.76, 3.74)
    val preciosVenta = listOf(3.78, 3.80, 3.77, 3.81, 3.79, 3.82, 3.80)
    val timestamps = listOf("10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00")

    val maxCompra = preciosCompra.maxOrNull() ?: 0.0
    val minVenta = preciosVenta.minOrNull() ?: 0.0
    val margenGeneral = "%.2f".format(minVenta - maxCompra)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Fila de título con el menú desplegable condicional
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "USD / PEN - Gráfico de Mercado",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Si el usuario inició sesión, se muestra el menú a la derecha
            if (usuarioInicioSesion) {
                Box {
                    IconButton(onClick = { menuAccionesDesplegado = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opciones de orden")
                    }
                    DropdownMenu(
                        expanded = menuAccionesDesplegado,
                        onDismissRequest = { menuAccionesDesplegado = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Libro de órdenes", fontWeight = FontWeight.Bold) },
                            onClick = { menuAccionesDesplegado = false }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Generar orden de compra") }, onClick = { menuAccionesDesplegado = false })
                        DropdownMenuItem(text = { Text("Generar oferta de venta") }, onClick = { menuAccionesDesplegado = false })
                        DropdownMenuItem(text = { Text("Comprar inmediatamente") }, onClick = { menuAccionesDesplegado = false })
                        DropdownMenuItem(text = { Text("Vender inmediatamente") }, onClick = { menuAccionesDesplegado = false })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selector de rangos de tiempo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            rangosTiempo.forEach { rango ->
                val esSeleccionado = rango == rangoTiempoSeleccionado
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (esSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { rangoTiempoSeleccionado = rango }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rango.replace("Último ", "Ú. ").replace("Última ", "Ú. "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (esSeleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Lienzo del gráfico interactivo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val anchoSeccion = size.width / (preciosCompra.size - 1)
                        puntoInteractuado = (offset.x / anchoSeccion).toInt().coerceIn(0, preciosCompra.size - 1)
                    }
                }
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ancho = size.width
                val alto = size.height
                val espacioX = ancho / (preciosCompra.size - 1)
                val minDato = 3.70
                val maxDato = 3.80
                val rangoDato = maxDato - minDato

                val pathCompra = Path()
                val pathVenta = Path()

                preciosCompra.forEachIndexed { index, precio ->
                    val x = index * espacioX
                    val y = alto - ((precio - minDato) / rangoDato * alto).toFloat()
                    if (index == 0) pathCompra.moveTo(x, y) else pathCompra.lineTo(x, y)
                }

                preciosVenta.forEachIndexed { index, precio ->
                    val x = index * espacioX
                    val y = alto - ((precio - minDato) / rangoDato * alto).toFloat()
                    if (index == 0) pathVenta.moveTo(x, y) else pathVenta.lineTo(x, y)
                }

                drawPath(path = pathCompra, color = Color(0xFF2563EB), style = Stroke(width = 3.dp.toPx()))
                drawPath(path = pathVenta, color = Color(0xFF16A34A), style = Stroke(width = 3.dp.toPx()))

                puntoInteractuado?.let { index ->
                    drawLine(color = Color.Gray, start = Offset(index * espacioX, 0f), end = Offset(index * espacioX, alto), strokeWidth = 1.dp.toPx())
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cuadro informativo al interactuar
        puntoInteractuado?.let { index ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Detalle (${timestamps[index]} hrs):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "Mayor Compra: S/ ${preciosCompra[index]}", color = Color(0xFF2563EB), fontSize = 13.sp)
                    Text(text = "Menor Venta: S/ ${preciosVenta[index]}", color = Color(0xFF16A34A), fontSize = 13.sp)
                    Text(text = "Margen: S/ ${"%.2f".format(preciosVenta[index] - preciosCompra[index])}", color = Color(0xFFEA580C), fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(text = "Resumen General", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text(text = "Mayor precio de compra: ", fontSize = 16.sp)
                Text(text = "S/ $maxCompra", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
            }
            Row {
                Text(text = "Menor precio de venta: ", fontSize = 16.sp)
                Text(text = "S/ $minVenta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
            }
            Row {
                Text(text = "Margen de ganancia: ", fontSize = 16.sp)
                Text(text = "S/ $margenGeneral", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
            }
        }
    }
}