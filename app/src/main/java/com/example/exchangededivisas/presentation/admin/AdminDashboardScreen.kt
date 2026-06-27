package com.example.exchangededivisas.presentation.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.exchangededivisas.data.repository.AdminMetricasUi
import com.example.exchangededivisas.data.repository.AdminRepository
import com.example.exchangededivisas.data.repository.AdminUserUi
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController, userRole: String = "ADM") {

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

    // Estado global de datos
    var metricas by remember { mutableStateOf<AdminMetricasUi?>(null) }
    var usuarios by remember { mutableStateOf<List<AdminUserUi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Carga inicial
    LaunchedEffect(Unit) {
        isLoading = true
        errorMsg = null
        runCatching {
            metricas = AdminRepository.loadMetricas().getOrThrow()
            usuarios = AdminRepository.loadUsuarios().getOrThrow()
        }.onFailure {
            errorMsg = it.message ?: "Error al cargar datos"
        }
        isLoading = false
    }

    fun reload() {
        scope.launch {
            isLoading = true
            errorMsg = null
            runCatching {
                metricas = AdminRepository.loadMetricas().getOrThrow()
                usuarios = AdminRepository.loadUsuarios().getOrThrow()
            }.onFailure {
                errorMsg = it.message ?: "Error al recargar"
            }
            isLoading = false
        }
    }

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
                HorizontalDivider()

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

                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        AppSession.logout()
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
                    actions = {
                        IconButton(onClick = { reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recargar")
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
                when {
                    isLoading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cargando datos de Supabase...")
                        }
                    }
                    errorMsg != null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = errorMsg!!,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { reload() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                    pantallaActual == "Visión general de operaciones" -> {
                        VisionGeneralOperaciones(metricas)
                    }
                    else -> {
                        GestionUsuarios(
                            usuarios = usuarios,
                            adminId = AppSession.currentUser.value.usuarioId,
                            onToggleRestriccion = { usuario, mensaje ->
                                scope.launch {
                                    AdminRepository.toggleRestriccion(
                                        adminId = AppSession.currentUser.value.usuarioId,
                                        usuario = usuario,
                                        mensaje = mensaje
                                    )
                                    reload()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VisionGeneralOperaciones(metricas: AdminMetricasUi?) {
    if (metricas == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin datos disponibles")
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            MetricCard(
                "Usuarios Registrados",
                metricas.totalUsuarios.toString(),
                "Total en el sistema"
            )
        }
        item {
            MetricCard(
                "Órdenes Activas",
                metricas.ordenesActivas.toString(),
                "Órdenes y ofertas en mercado"
            )
        }
        item {
            MetricCard(
                "Transacciones Hoy",
                metricas.transaccionesHoy.toString(),
                "Ejecuciones del día"
            )
        }
        item {
            MetricCard(
                "Volumen Hoy",
                "%.2f".format(metricas.volumenHoy),
                "Total operado hoy"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionUsuarios(
    usuarios: List<AdminUserUi>,
    adminId: Int,
    onToggleRestriccion: (AdminUserUi, String) -> Unit
) {
    var filtroNombre by remember { mutableStateOf("") }
    var filtroCorreo by remember { mutableStateOf("") }
    var filtroEstado by remember { mutableStateOf("Todos") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var usuarioParaDetalle by remember { mutableStateOf<AdminUserUi?>(null) }
    var usuarioParaAccion by remember { mutableStateOf<AdminUserUi?>(null) }
    var mensajeAccion by remember { mutableStateOf("") }

    val usuariosFiltrados = usuarios.filter { u ->
        val coincideNombre = u.nombre.contains(filtroNombre, ignoreCase = true)
        val coincideCorreo = u.correo.contains(filtroCorreo, ignoreCase = true)
        val coincideEstado = when (filtroEstado) {
            "Restringido" -> u.estaRestringido
            "No restringido" -> !u.estaRestringido
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
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = filtroEstado,
                onValueChange = {},
                readOnly = true,
                label = { Text("Estado de cuenta") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                listOf("Todos", "Restringido", "No restringido").forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion) },
                        onClick = { filtroEstado = opcion; dropdownExpanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(usuariosFiltrados) { usuario ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { usuarioParaDetalle = usuario },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = usuario.nombre,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = usuario.correo,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = if (usuario.estaRestringido) "Restringido" else "Activo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (usuario.estaRestringido)
                                    Color(0xFFFF6B6B) else Color(0xFF4ADE80),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Button(
                            onClick = {
                                usuarioParaAccion = usuario
                                mensajeAccion = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (usuario.estaRestringido)
                                    Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        ) {
                            Text(if (usuario.estaRestringido) "Habilitar" else "Restringir")
                        }
                    }
                }
            }
        }
    }

    // Diálogo: Ver billetera e historial
    if (usuarioParaDetalle != null) {
        val u = usuarioParaDetalle!!
        AlertDialog(
            onDismissRequest = { usuarioParaDetalle = null },
            title = { Text("Información de ${u.nombre}") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Registrado: ${u.fechaRegistro}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Saldos de Billetera:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (u.saldos.isEmpty()) {
                        Text(
                            "Sin saldos registrados.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    } else {
                        u.saldos.forEach { (moneda, monto) ->
                            Text(
                                "$moneda: ${"%.2f".format(monto)}",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Historial de Transacciones:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (u.historial.isEmpty()) {
                        Text(
                            "Sin transacciones registradas.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    } else {
                        u.historial.forEach { tx ->
                            Column(
                                modifier = Modifier.padding(
                                    start = 8.dp, top = 4.dp, bottom = 4.dp
                                )
                            ) {
                                Text(
                                    "${tx.fecha} — ${tx.tipo}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    tx.detalle,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    "Estado: ${tx.estado}",
                                    fontSize = 11.sp,
                                    color = if (tx.estado == "Completada")
                                        Color(0xFF4ADE80) else Color(0xFFFF6B6B)
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { usuarioParaDetalle = null }) { Text("Cerrar") }
            }
        )
    }

    // Diálogo: Restricción / Habilitación
    if (usuarioParaAccion != null) {
        val u = usuarioParaAccion!!
        val esRestringir = !u.estaRestringido

        AlertDialog(
            onDismissRequest = { usuarioParaAccion = null },
            title = {
                Text(if (esRestringir) "Restringir cuenta" else "Habilitar cuenta")
            },
            text = {
                Column {
                    Text(
                        text = "Escriba el motivo para notificar al usuario:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = mensajeAccion,
                        onValueChange = { mensajeAccion = it },
                        label = { Text("Motivo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleRestriccion(u, mensajeAccion)
                        usuarioParaAccion = null
                    },
                    enabled = mensajeAccion.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (esRestringir)
                            Color(0xFFEE1919) else Color(0xFF06D391)
                    )
                ) {
                    Text("Confirmar")
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtexto,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}