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

@Composable
fun GestionUsuarios() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Lista y Control de Usuarios", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Aquí se listarán los usuarios registrados en el sistema.", fontSize = 14.sp)
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