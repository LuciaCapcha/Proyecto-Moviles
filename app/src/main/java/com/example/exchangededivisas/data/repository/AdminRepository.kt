package com.example.exchangededivisas.data.repository

import com.example.exchangededivisas.api.ApiClient
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class AdminUserUi(
    val usuarioId: Int,
    val nombre: String,
    val correo: String,
    val estado: String,
    val estaRestringido: Boolean,
    val fechaRegistro: String,
    val saldos: Map<String, Double>,
    val historial: List<AdminTransaccionUi>
)

data class AdminTransaccionUi(
    val fecha: String,
    val tipo: String,
    val detalle: String,
    val estado: String
)

data class AdminMetricasUi(
    val totalUsuarios: Int,
    val ordenesActivas: Int,
    val transaccionesHoy: Int,
    val volumenHoy: Double
)

object AdminRepository {

    private val api = ApiClient.supabase

    private fun nowIso(): String =
        OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    private fun formatFecha(value: String?): String {
        if (value.isNullOrBlank()) return "-"
        return value.take(16).replace("T", " ")
    }

    suspend fun loadMetricas(): Result<AdminMetricasUi> {
        return runCatching {
            val usuarios = api.getAllUsuarios()
            val ordenes = api.countOrdenesActivas()
            val ofertas = api.countOfertasActivas()

            val hoyPrefix = LocalDate.now().toString()
            val ejecucionesHoy = api.getEjecucionesHoy("gte.$hoyPrefix")
            val volumenHoy = ejecucionesHoy.sumOf {
                (it["totaloperacion"] as? Double) ?: 0.0
            }

            AdminMetricasUi(
                totalUsuarios = usuarios.size,
                ordenesActivas = ordenes.size + ofertas.size,
                transaccionesHoy = ejecucionesHoy.size,
                volumenHoy = volumenHoy
            )
        }
    }

    suspend fun loadUsuarios(): Result<List<AdminUserUi>> {
        return runCatching {
            val usuarios = api.getAllUsuarios()

            usuarios.map { u ->
                // Saldos
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

                // Historial
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

                AdminUserUi(
                    usuarioId = u.usuarioId,
                    nombre = u.nombreUsuario ?: "Sin nombre",
                    correo = u.correoElectronico ?: "-",
                    estado = u.estado ?: "Activo",
                    estaRestringido = u.estado?.equals("Restringido", ignoreCase = true) == true,
                    fechaRegistro = formatFecha(u.fechaRegistro),
                    saldos = saldos,
                    historial = historial
                )
            }
        }
    }

    suspend fun toggleRestriccion(
        adminId: Int,
        usuario: AdminUserUi,
        mensaje: String
    ): Result<String> {
        return runCatching {
            val nuevoEstado = if (usuario.estaRestringido) "Activo" else "Restringido"
            val tipoAccion = if (usuario.estaRestringido) "Habilitación" else "Restricción"
            val now = nowIso()

            // 1. Actualizar estado del usuario
            api.updateUsuarioEstado(
                usuarioId = "eq.${usuario.usuarioId}",
                body = mapOf("estado" to nuevoEstado)
            )

            // 2. Registrar en RestriccionesUsuario
            api.insertRestriccion(
                mapOf(
                    "usuarioid" to usuario.usuarioId,
                    "administradorid" to adminId,
                    "tipoaccion" to tipoAccion,
                    "mensaje" to mensaje,
                    "fechainicio" to now,
                    "fechafin" to null,
                    "estadorestriccion" to "Activa"
                )
            )

            // 3. Registrar en AuditoriaAdministrativa
            api.insertAuditoria(
                mapOf(
                    "administradorid" to adminId,
                    "usuarioafectadoid" to usuario.usuarioId,
                    "tipoaccion" to tipoAccion,
                    "mensajeregistrado" to mensaje,
                    "fechahora" to now
                )
            )

            // 4. Notificar al usuario por correo
            api.insertNotificacionCorreo(
                mapOf(
                    "usuarioid" to usuario.usuarioId,
                    "tiponotificacionid" to null,
                    "correodestino" to usuario.correo,
                    "tipoevento" to "CAMBIO_ESTADO_CUENTA",
                    "asunto" to "Tu cuenta ha sido $tipoAccion",
                    "cuerpo" to "Motivo: $mensaje",
                    "estadoenvio" to "Pendiente",
                    "fechacreacion" to now,
                    "fechaenvio" to null,
                    "referenciatipo" to null,
                    "referenciaid" to null
                )
            )

            "Usuario ${if (usuario.estaRestringido) "habilitado" else "restringido"} correctamente."
        }
    }
}