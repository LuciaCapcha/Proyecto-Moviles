package com.example.exchangededivisas.presentation.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.exchangededivisas.data.repository.AdminMetricasUi
import com.example.exchangededivisas.data.repository.AdminRepository
import com.example.exchangededivisas.data.repository.AdminRestrictionUi
import com.example.exchangededivisas.data.repository.AdminUserUi
import com.example.exchangededivisas.data.repository.AiAdminMessageRepository
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    userRole: String = "ADM",
    initialScreen: String = "Visión general de operaciones"
) {
    if (userRole != "ADM") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Acceso rechazado. No tienes permisos de administrador.",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        return
    }

    val scope = rememberCoroutineScope()
    var metricas by remember { mutableStateOf<AdminMetricasUi?>(null) }
    var usuarios by remember { mutableStateOf<List<AdminUserUi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

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

    LaunchedEffect(Unit) { reload() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (initialScreen == "Gestión de usuarios") "Gestión de usuarios" else "Visión general de operaciones",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Panel administrativo",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Recargar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (actionMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = actionMessage!!,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            isLoading -> LoadingAdminPanel()
            errorMsg != null -> ErrorAdminPanel(errorMsg = errorMsg!!, onRetry = { reload() })
            initialScreen == "Gestión de usuarios" -> {
                GestionUsuarios(
                    usuarios = usuarios,
                    adminId = AppSession.currentUser.value.usuarioId,
                    onToggleRestriccion = { usuario, mensaje ->
                        scope.launch {
                            isLoading = true
                            val result = AdminRepository.toggleRestriccion(
                                adminId = AppSession.currentUser.value.usuarioId,
                                usuario = usuario,
                                mensaje = mensaje
                            )
                            actionMessage = result.getOrElse { it.message ?: "No se pudo completar la acción." }
                            metricas = AdminRepository.loadMetricas().getOrNull()
                            usuarios = AdminRepository.loadUsuarios().getOrDefault(emptyList())
                            isLoading = false
                        }
                    }
                )
            }
            else -> VisionGeneralOperaciones(metricas)
        }
    }
}

@Composable
private fun LoadingAdminPanel() {
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

@Composable
private fun ErrorAdminPanel(errorMsg: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = errorMsg,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    MetricCard("Usuarios registrados", metricas.totalUsuarios.toString(), "Total en el sistema")
                }
                Box(modifier = Modifier.weight(1f)) {
                    MetricCard("Órdenes activas", metricas.ordenesActivas.toString(), "Órdenes y ofertas en mercado")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    MetricCard("Transacciones hoy", metricas.transaccionesHoy.toString(), "Conteo desde historial del día")
                }
                Box(modifier = Modifier.weight(1f)) {
                    MetricCard("Volumen hoy", "%.2f".format(metricas.volumenHoy), "Desglose por moneda debajo")
                }
            }
        }

        item {
            Text(
                text = "Volumen operado hoy por moneda",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (metricas.volumenPorMoneda.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "Aún no hay volumen registrado hoy.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(metricas.volumenPorMoneda) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.codigoIso, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("%.4f".format(item.monto), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
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
    var isGeneratingAiMessage by remember { mutableStateOf(false) }
    var isSubmittingAction by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
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
                        onClick = {
                            filtroEstado = opcion
                            dropdownExpanded = false
                        }
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
                UserAdminCard(
                    usuario = usuario,
                    adminId = adminId,
                    onClick = { usuarioParaDetalle = usuario },
                    onActionClick = {
                        usuarioParaAccion = usuario
                        mensajeAccion = ""
                        aiError = null
                    }
                )
            }
        }
    }

    usuarioParaDetalle?.let { u ->
        UserDetailDialog(
            usuario = u,
            onDismiss = { usuarioParaDetalle = null }
        )
    }

    usuarioParaAccion?.let { u ->
        val esRestringir = !u.estaRestringido
        val cannotAct = u.esAdministrador || u.usuarioId == adminId

        AlertDialog(
            onDismissRequest = { if (!isSubmittingAction) usuarioParaAccion = null },
            title = { Text(if (esRestringir) "Restringir cuenta" else "Habilitar cuenta") },
            text = {
                Column {
                    if (cannotAct) {
                        Text(
                            text = if (u.usuarioId == adminId) "No puedes modificar tu propia cuenta de administrador." else "No se puede restringir ni habilitar a otro administrador.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Escriba el motivo para notificar al usuario:",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isGeneratingAiMessage = true
                                    aiError = null
                                    AiAdminMessageRepository.generateRestrictionMessage(
                                        usuario = u,
                                        restrict = esRestringir,
                                        draftMessage = mensajeAccion
                                    ).onSuccess { generated -> mensajeAccion = generated }
                                        .onFailure { error -> aiError = error.message ?: "No se pudo generar el mensaje con IA." }
                                    isGeneratingAiMessage = false
                                }
                            },
                            enabled = !isGeneratingAiMessage && !isSubmittingAction,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (isGeneratingAiMessage) "Procesando con IA..."
                                else if (mensajeAccion.isBlank()) "Generar mensaje con IA"
                                else "Mejorar mensaje con IA"
                            )
                        }

                        if (aiError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = aiError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = mensajeAccion,
                            onValueChange = { mensajeAccion = it.take(300) },
                            label = { Text("Motivo") },
                            supportingText = { Text("${mensajeAccion.length}/300") },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingAction = true
                        onToggleRestriccion(u, mensajeAccion)
                        usuarioParaAccion = null
                        isSubmittingAction = false
                    },
                    enabled = !cannotAct && mensajeAccion.isNotBlank() && !isSubmittingAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (esRestringir) Color(0xFFEE1919) else Color(0xFF06D391)
                    )
                ) { Text(if (isSubmittingAction) "Procesando..." else "Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { usuarioParaAccion = null }, enabled = !isSubmittingAction) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun UserAdminCard(
    usuario: AdminUserUi,
    adminId: Int,
    onClick: () -> Unit,
    onActionClick: () -> Unit
) {
    val cannotAct = usuario.esAdministrador || usuario.usuarioId == adminId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    text = when {
                        usuario.esAdministrador -> "Administrador"
                        usuario.estaRestringido -> "Restringido"
                        else -> "Activo"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        usuario.esAdministrador -> MaterialTheme.colorScheme.primary
                        usuario.estaRestringido -> Color(0xFFFF6B6B)
                        else -> Color(0xFF4ADE80)
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Acciones administrativas: ${usuario.historialRestricciones.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                usuario.historialRestricciones.firstOrNull()?.let { lastAction ->
                    Text(
                        text = "Última: ${lastAction.fechaInicio} — ${lastAction.tipoAccion}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                TextButton(onClick = onClick) {
                    Text("Ver detalle e historial")
                }
            }

            Button(
                onClick = onActionClick,
                enabled = !cannotAct,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (usuario.estaRestringido) Color(0xFF10B981) else Color(0xFFEF4444),
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Text(if (cannotAct) "Protegido" else if (usuario.estaRestringido) "Habilitar" else "Restringir")
            }
        }
    }
}

@Composable
private fun UserDetailDialog(
    usuario: AdminUserUi,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Información de ${usuario.nombre}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Registrado: ${usuario.fechaRegistro}", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                Text("Saldos de billetera", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (usuario.saldos.isEmpty()) {
                    Text("Sin saldos registrados.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                } else {
                    usuario.saldos.forEach { (moneda, monto) ->
                        Text("$moneda: ${"%.2f".format(monto)}", fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Historial de transacciones", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (usuario.historial.isEmpty()) {
                    Text("Sin transacciones registradas.", fontSize = 13.sp, color = Color.Gray)
                } else {
                    usuario.historial.take(15).forEach { tx ->
                        Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                            Text("${tx.fecha} — ${tx.tipo}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(tx.detalle, fontSize = 11.sp, color = Color.Gray)
                            Text("Estado: ${tx.estado}", fontSize = 11.sp)
                        }
                        HorizontalDivider()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Historial de restricciones y habilitaciones", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (usuario.historialRestricciones.isEmpty()) {
                    Text("Sin restricciones ni habilitaciones registradas para este usuario.", fontSize = 13.sp, color = Color.Gray)
                } else {
                    usuario.historialRestricciones.forEach { r -> RestrictionHistoryRow(r) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun RestrictionHistoryRow(item: AdminRestrictionUi) {
    Column(modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp)) {
        Text(
            text = "${item.fechaInicio} — ${item.tipoAccion}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Estado: ${item.estadoRestriccion}${if (item.fechaFin != "-") " | Fin: ${item.fechaFin}" else ""}",
            fontSize = 11.sp,
            color = Color.Gray
        )
        Text(
            text = "Registrado en: ${item.fuente} | Admin ID: ${item.administradorId}",
            fontSize = 10.sp,
            color = Color.Gray
        )
        Text(
            text = item.mensaje,
            fontSize = 12.sp
        )
    }
    HorizontalDivider()
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
