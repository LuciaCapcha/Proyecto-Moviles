package com.example.exchangededivisas.data.repository

import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.data.remote.HistorialTransaccionDto
import com.example.exchangededivisas.data.remote.AuditoriaAdministrativaDto
import com.example.exchangededivisas.data.notification.NativeNotificationBus
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

data class AdminUserUi(
    val usuarioId: Int,
    val rolId: Int,
    val nombre: String,
    val correo: String,
    val estado: String,
    val estaRestringido: Boolean,
    val esAdministrador: Boolean,
    val fechaRegistro: String,
    val saldos: Map<String, Double>,
    val historial: List<AdminTransaccionUi>,
    val historialRestricciones: List<AdminRestrictionUi>
)

data class AdminTransaccionUi(
    val fecha: String,
    val tipo: String,
    val detalle: String,
    val estado: String
)

data class AdminRestrictionUi(
    val fechaInicio: String,
    val fechaFin: String,
    val tipoAccion: String,
    val estadoRestriccion: String,
    val mensaje: String,
    val administradorId: Int,
    val fuente: String = "restriccionesusuario"
)

data class AdminVolumeMonedaUi(
    val codigoIso: String,
    val monto: Double
)

data class AdminMetricasUi(
    val totalUsuarios: Int,
    val ordenesActivas: Int,
    val transaccionesHoy: Int,
    val volumenHoy: Double,
    val volumenPorMoneda: List<AdminVolumeMonedaUi> = emptyList()
)

object AdminRepository {

    private val api = ApiClient.supabase

    private fun nowIso(): String =
        OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    private fun todayStartIso(): String {
        val now = OffsetDateTime.now()
        return now.toLocalDate().atStartOfDay().atOffset(now.offset)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun formatFecha(value: String?): String {
        if (value.isNullOrBlank()) return "-"
        return value.take(16).replace("T", " ")
    }

    suspend fun loadMetricas(): Result<AdminMetricasUi> {
        return runCatching {
            val usuarios = api.getAllUsuarios()
            val ordenes = api.countOrdenesActivas()
            val ofertas = api.countOfertasActivas()
            val historialHoy = api.getHistorialDesde("gte.${todayStartIso()}")

            val volumenPorMoneda = mutableMapOf<String, Double>()
            historialHoy.forEach { history ->
                resolveHistoryVolumeByCurrency(history).forEach { (codigo, monto) ->
                    if (monto > 0.0) {
                        volumenPorMoneda[codigo] = (volumenPorMoneda[codigo] ?: 0.0) + monto
                    }
                }
            }

            val volumenes = volumenPorMoneda
                .map { AdminVolumeMonedaUi(it.key, it.value) }
                .sortedByDescending { it.monto }

            AdminMetricasUi(
                totalUsuarios = usuarios.size,
                ordenesActivas = ordenes.size + ofertas.size,
                transaccionesHoy = historialHoy.size,
                volumenHoy = volumenes.sumOf { it.monto },
                volumenPorMoneda = volumenes
            )
        }
    }

    private suspend fun resolveHistoryVolumeByCurrency(history: HistorialTransaccionDto): Map<String, Double> {
        return runCatching {
            val values = mutableMapOf<String, Double>()

            suspend fun add(monedaId: Int?, amount: Double?) {
                val safeAmount = amount ?: 0.0
                if (monedaId == null || safeAmount <= 0.0) return
                val code = api.getMonedaById("eq.$monedaId").firstOrNull()?.codigoIso?.trim() ?: return
                values[code] = (values[code] ?: 0.0) + safeAmount
            }

            suspend fun addPairVolumes(parMonedaId: Int, originAmount: Double?, destinationAmount: Double?) {
                val pair = api.getParMonedaById("eq.$parMonedaId").firstOrNull() ?: return
                add(pair.monedaOrigenId, originAmount)
                add(pair.monedaDestinoId, destinationAmount)
            }

            when (history.tipoOperacion) {
                "Compra inmediata", "Venta inmediata" -> {
                    val op = api.getOperacionInmediataById("eq.${history.referenciaId}").firstOrNull()
                    if (op != null) {
                        addPairVolumes(
                            parMonedaId = op.parMonedaId,
                            originAmount = op.totalPagado,
                            destinationAmount = op.totalRecibido
                        )
                    }
                }

                "Deposito" -> {
                    val deposito = api.getDepositoById("eq.${history.referenciaId}").firstOrNull()
                    add(deposito?.monedaId, deposito?.montoDepositado)
                }

                "Retiro" -> {
                    val retiro = api.getRetiroById("eq.${history.referenciaId}").firstOrNull()
                    add(retiro?.monedaId, retiro?.montoRetirado)
                }

                "Orden de compra" -> {
                    val order = api.getOrdenCompraById("eq.${history.referenciaId}").firstOrNull()
                    if (order != null) {
                        addPairVolumes(
                            parMonedaId = order.parMonedaId,
                            originAmount = order.totalEjecutado,
                            destinationAmount = order.cantidadObtenida
                        )
                    }
                }

                "Oferta de venta" -> {
                    val offer = api.getOfertaVentaById("eq.${history.referenciaId}").firstOrNull()
                    if (offer != null) {
                        addPairVolumes(
                            parMonedaId = offer.parMonedaId,
                            originAmount = offer.totalRecibido,
                            destinationAmount = offer.cantidadVendida
                        )
                    }
                }

                "Cancelacion" -> {
                    val cancellation = api.getCancelacionById("eq.${history.referenciaId}").firstOrNull()
                    if (cancellation != null) {
                        val pair = api.getParMonedaById("eq.${cancellation.parMonedaId}").firstOrNull()
                        val refundCurrencyId = if (cancellation.tipoOperacion == "Orden de compra") {
                            pair?.monedaOrigenId
                        } else {
                            pair?.monedaDestinoId
                        }
                        add(refundCurrencyId, cancellation.montoReembolsado)
                    }
                }
            }

            values
        }.getOrDefault(emptyMap())
    }

    suspend fun loadUsuarios(): Result<List<AdminUserUi>> {
        return runCatching {
            val usuarios = api.getAllUsuarios()

            usuarios.map { u ->
                val saldos = runCatching {
                    val billetera = api.getBilleteraByUser("eq.${u.usuarioId}").firstOrNull()
                    if (billetera != null) {
                        val saldoList = api.getSaldosByWallet("eq.${billetera.billeteraId}")
                        saldoList.associate { s ->
                            val moneda = api.getMonedaById("eq.${s.monedaId}")
                                .firstOrNull()?.codigoIso?.trim() ?: "?"
                            moneda to s.saldoDisponible
                        }
                    } else emptyMap()
                }.getOrDefault(emptyMap())

                val historial = runCatching {
                    api.getHistorialByUsuario("eq.${u.usuarioId}").map { h ->
                        AdminTransaccionUi(
                            fecha = formatFecha(h.fechaHora),
                            tipo = h.tipoOperacion,
                            detalle = "Ref: ${h.referenciaId} | ${h.metodoEjecucion ?: "-"}",
                            estado = h.estado
                        )
                    }
                }.getOrDefault(emptyList())

                val restricciones = loadRestrictionHistoryForUser(u.usuarioId)

                AdminUserUi(
                    usuarioId = u.usuarioId,
                    rolId = u.rolId ?: 1,
                    nombre = u.nombreUsuario ?: "Sin nombre",
                    correo = u.correoElectronico ?: "-",
                    estado = u.estado ?: "Activo",
                    estaRestringido = u.estado?.equals("Restringido", ignoreCase = true) == true,
                    esAdministrador = (u.rolId ?: 1) == 2,
                    fechaRegistro = formatFecha(u.fechaRegistro),
                    saldos = saldos,
                    historial = historial,
                    historialRestricciones = restricciones
                )
            }.sortedBy { it.nombre.lowercase() }
        }
    }

    private suspend fun loadRestrictionHistoryForUser(usuarioId: Int): List<AdminRestrictionUi> {
        val desdeRestricciones = runCatching {
            api.getRestriccionesByUsuario("eq.$usuarioId").map { r ->
                AdminRestrictionUi(
                    fechaInicio = formatFecha(r.fechaInicio),
                    fechaFin = formatFecha(r.fechaFin),
                    tipoAccion = r.tipoAccion,
                    estadoRestriccion = r.estadoRestriccion,
                    mensaje = r.mensaje,
                    administradorId = r.administradorId,
                    fuente = "restriccionesusuario"
                )
            }
        }.getOrDefault(emptyList())

        val desdeAuditoria = runCatching {
            api.getAuditoriaByUsuario("eq.$usuarioId").map { a ->
                AdminRestrictionUi(
                    fechaInicio = formatFecha(a.fechaHora),
                    fechaFin = "-",
                    tipoAccion = a.tipoAccion,
                    estadoRestriccion = "Registrada",
                    mensaje = a.mensajeRegistrado ?: "Sin mensaje registrado.",
                    administradorId = a.administradorId,
                    fuente = "auditoriaadministrativa"
                )
            }
        }.getOrDefault(emptyList())

        val auditoriaSinDuplicar = desdeAuditoria.filter { audit ->
            desdeRestricciones.none { restriction ->
                restriction.tipoAccion == audit.tipoAccion &&
                    restriction.mensaje.trim() == audit.mensaje.trim() &&
                    restriction.fechaInicio == audit.fechaInicio
            }
        }

        return (desdeRestricciones + auditoriaSinDuplicar)
            .sortedByDescending { it.fechaInicio }
    }

    suspend fun toggleRestriccion(
        adminId: Int,
        usuario: AdminUserUi,
        mensaje: String
    ): Result<String> {
        return runCatching {
            require(mensaje.isNotBlank()) { "El mensaje no puede estar vacío." }
            require(usuario.usuarioId != adminId) { "Un administrador no puede restringirse ni habilitarse a sí mismo." }
            require(!usuario.esAdministrador) { "No se puede restringir ni habilitar a otro administrador." }

            val admin = api.getUsuarioById("eq.$adminId").firstOrNull()
                ?: error("No se encontró el administrador autenticado.")
            require((admin.rolId ?: 1) == 2) { "Solo un administrador puede realizar esta acción." }

            val usuarioActual = api.getUsuarioById("eq.${usuario.usuarioId}").firstOrNull()
                ?: error("No se encontró el usuario seleccionado.")
            val actualmenteRestringido = usuarioActual.estado?.equals("Restringido", ignoreCase = true) == true

            val nuevoEstado = if (actualmenteRestringido) "Activo" else "Restringido"
            val tipoAccion = if (actualmenteRestringido) "Habilitación" else "Restricción"
            val now = nowIso()

            if (!actualmenteRestringido) {
                ExchangeRepository.loadActiveTransactions(usuario.usuarioId)
                    .getOrDefault(emptyList())
                    .forEach { active ->
                        ExchangeRepository.cancelActiveTransaction(usuario.usuarioId, active).getOrNull()
                    }
            } else {
                api.updateRestriccionesActivasByUsuario(
                    usuarioId = "eq.${usuario.usuarioId}",
                    body = mapOf(
                        "fechafin" to now,
                        "estadorestriccion" to "Finalizada"
                    )
                )
            }

            api.updateUsuarioEstado(
                usuarioId = "eq.${usuario.usuarioId}",
                body = mapOf("estado" to nuevoEstado)
            )

            api.insertRestriccion(
                mapOf(
                    "usuarioid" to usuario.usuarioId,
                    "administradorid" to adminId,
                    "tipoaccion" to tipoAccion,
                    "mensaje" to mensaje.trim(),
                    "fechainicio" to now,
                    "fechafin" to if (actualmenteRestringido) now else null,
                    "estadorestriccion" to if (actualmenteRestringido) "Finalizada" else "Activa"
                )
            )

            api.insertAuditoria(
                mapOf(
                    "administradorid" to adminId,
                    "usuarioafectadoid" to usuario.usuarioId,
                    "tipoaccion" to tipoAccion,
                    "mensajeregistrado" to mensaje.trim(),
                    "fechahora" to now
                )
            )

            val asunto = if (actualmenteRestringido) {
                "Tu cuenta ha sido habilitada"
            } else {
                "Tu cuenta ha sido restringida"
            }
            val cuerpo = "Motivo: ${mensaje.trim()}"

            val notificacion = api.insertNotificacionCorreo(
                mapOf(
                    "usuarioid" to usuario.usuarioId,
                    "tiponotificacionid" to null,
                    "correodestino" to usuario.correo,
                    "tipoevento" to "CAMBIO_ESTADO_CUENTA",
                    "asunto" to asunto,
                    "cuerpo" to cuerpo,
                    "estadoenvio" to "Pendiente",
                    "fechacreacion" to now,
                    "fechaenvio" to null,
                    "referenciatipo" to "restriccionesusuario",
                    "referenciaid" to null
                )
            ).firstOrNull()

            NativeNotificationBus.emitForUser(
                recipientUserId = usuario.usuarioId,
                title = asunto,
                body = cuerpo,
                notificationId = notificacion?.notificacionId
            )
            runCatching { EmailDispatchRepository.flushPendingEmails() }

            "Usuario ${if (actualmenteRestringido) "habilitado" else "restringido"} correctamente."
        }
    }
}
