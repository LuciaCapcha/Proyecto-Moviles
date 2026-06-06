package com.example.exchangededivisas.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController, userRole: String = "ADM") {
    // Criterio de aceptación: Restringido al rol ADM
    if (userRole != "ADM") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Acceso Rechazado. No tienes permisos de Administrador.",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var pantallaActual by remember { mutableStateOf("Visión general de operaciones") }

    // Criterio de aceptación: Barra lateral izquierda para navegar
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Panel Administrativo",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Divider()

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    label = { Text("Visión general de operaciones") },
                    selected = pantallaActual == "Visión general de operaciones",
                    onClick = {
                        pantallaActual = "Visión general de operaciones"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    label = { Text("Gestión de usuarios") },
                    selected = pantallaActual == "Gestión de usuarios",
                    onClick = {
                        pantallaActual = "Gestión de usuarios"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Espaciador para empujar el botón de salida hacia el fondo de la barra lateral
                Spacer(modifier = Modifier.weight(1f))

                // Botón para Cerrar Sesión y volver al Login
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // Regresa al login y borra el historial del panel de admin
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(pantallaActual) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Abrir menú")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (pantallaActual == "Visión general de operaciones") {
                    VisionGeneralOperaciones()
                } else {
                    GestionUsuarios()
                }
            }
        }
    }
}

@Composable
fun VisionGeneralOperaciones() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { MetricCard("Usuarios Registrados", "1,245", "Actualizado automáticamente") }
        item { MetricCard("Volumen Operado (USD)", "$45,200.50", "Actualizado automáticamente") }
        item { MetricCard("Órdenes Activas", "89", "Ofertas en mercado") }
        item { MetricCard("Transacciones Hoy", "312", "Ejecutadas con éxito") }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionUsuarios() {
    var filtroNombre by remember { mutableStateOf("") }
    var filtroCorreo by remember { mutableStateOf("") }
    var filtroEstado by remember { mutableStateOf("Todos") }
    var unirseOpcionesDesplegadas by remember { mutableStateOf(false) }

    // Estados para controlar qué usuario se seleccionó y qué ventana abrir
    var usuarioParaDetalle by remember { mutableStateOf<AdminUser?>(null) }
    var usuarioParaAccion by remember { mutableStateOf<AdminUser?>(null) }
    var mensajeAccion by remember { mutableStateOf("") }

    val usuariosFiltrados = MockAdminUsers.lista.filter { usuario ->
        val coincideNombre = usuario.nombre.contains(filtroNombre, ignoreCase = true)
        val coincideCorreo = usuario.correo.contains(filtroCorreo, ignoreCase = true)
        val coincideEstado = when (filtroEstado) {
            "Restringido" -> usuario.estaRestringido
            "No restringido" -> !usuario.estaRestringido
            else -> true
        }
        coincideNombre && coincideCorreo && coincideEstado
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Búsqueda y Control de Cuentas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // --- FILTROS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filtroNombre,
                onValueChange = { filtroNombre = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = filtroCorreo,
                onValueChange = { filtroCorreo = it },
                label = { Text("Correo") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = unirseOpcionesDesplegadas,
            onExpandedChange = { unirseOpcionesDesplegadas = !unirseOpcionesDesplegadas },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = filtroEstado,
                onValueChange = {},
                readOnly = true,
                label = { Text("Estado de cuenta") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unirseOpcionesDesplegadas) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = unirseOpcionesDesplegadas,
                onDismissRequest = { unirseOpcionesDesplegadas = false }
            ) {
                DropdownMenuItem(text = { Text("Todos") }, onClick = { filtroEstado = "Todos"; unirseOpcionesDesplegadas = false })
                DropdownMenuItem(text = { Text("Restringido") }, onClick = { filtroEstado = "Restringido"; unirseOpcionesDesplegadas = false })
                DropdownMenuItem(text = { Text("No restringido") }, onClick = { filtroEstado = "No restringido"; unirseOpcionesDesplegadas = false })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // --- LISTA ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(usuariosFiltrados) { usuario ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { usuarioParaDetalle = usuario }, // Criterio: Click abre detalles
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = usuario.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = usuario.correo, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = if (usuario.estaRestringido) "Estado: Restringido" else "Estado: Activo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (usuario.estaRestringido) Color(0xFFFF6B6B) else Color(0xFF4ADE80),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Button(
                            onClick = {
                                usuarioParaAccion = usuario
                                mensajeAccion = "" // Resetea el cuadro de texto
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (usuario.estaRestringido) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        ) {
                            Text(if (usuario.estaRestringido) "Habilitar" else "Restringir")
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGO 1: VER BILLETERA E HISTORIAL ---
    if (usuarioParaDetalle != null) {
        AlertDialog(
            onDismissRequest = { usuarioParaDetalle = null },
            title = { Text("Información de ${usuarioParaDetalle!!.nombre}") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Saldos de Billetera:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    usuarioParaDetalle!!.saldos.forEach { (moneda, monto) ->
                        Text("$moneda: $$monto", fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Historial de Transacciones:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (usuarioParaDetalle!!.historial.isEmpty()) {
                        Text("Sin transacciones registradas.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        usuarioParaDetalle!!.historial.forEach { tx ->
                            Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Text("${tx.fecha} - ${tx.tipo}: ${tx.monto}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(tx.detalle, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { usuarioParaDetalle = null }) { Text("Cerrar") }
            }
        )
    }

    // --- DIÁLOGO 2: MENSAJE DE RESTRICCIÓN / HABILITACIÓN ---
    if (usuarioParaAccion != null) {
        val esRestringir = !usuarioParaAccion!!.estaRestringido
        val tituloDialogo = if (esRestringir) "Restringir cuenta" else "Habilitar cuenta"

        AlertDialog(
            onDismissRequest = { usuarioParaAccion = null },
            title = { Text(tituloDialogo) },
            text = {
                Column {
                    Text(
                        text = "Escriba el motivo de la acción para notificar al usuario:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = mensajeAccion,
                        onValueChange = { mensajeAccion = it },
                        label = { Text("Mensaje o motivo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = MockAdminUsers.lista.indexOfFirst { it.id == usuarioParaAccion!!.id }
                        if (index != -1) {
                            // Modificamos la propiedad del objeto
                            val userActualizado = MockAdminUsers.lista[index]
                            userActualizado.estaRestringido = !userActualizado.estaRestringido

                            // SOLUCIÓN: Asignación directa con corchetes para refrescar la pantalla en Compose
                            MockAdminUsers.lista[index] = userActualizado
                        }
                        usuarioParaAccion = null
                    },
                    enabled = mensajeAccion.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (esRestringir) Color(0xFFEE1919) else Color(0xFF06D391)
                    )
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { usuarioParaAccion = null }) { Text("Cancelar") }
            }
        )
    }
}




@Composable
fun MetricCard(titulo: String, valor: String, subtexto: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = titulo, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = valor, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = subtexto, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}