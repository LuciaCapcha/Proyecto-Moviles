package com.example.exchangededivisas.data.repository

import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.data.remote.UsuarioDto
import com.example.exchangededivisas.data.session.AppSession
import com.example.exchangededivisas.data.session.CurrentUser
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object AuthRepository {

    private val api = ApiClient.supabase

    private fun nowIso(): String {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    suspend fun login(
        usernameOrEmail: String,
        password: String
    ): Result<CurrentUser> {
        return runCatching {
            val input = usernameOrEmail.trim()
            val rawPassword = password.trim()

            require(input.isNotBlank()) { "Ingrese usuario o correo." }
            require(rawPassword.isNotBlank()) { "Ingrese contraseña." }

            val user = findUser(input)
                ?: throw IllegalArgumentException("Credenciales inválidas")

            val isPasswordValid = user.passwordHash == rawPassword

            if (!isPasswordValid) {
                registerAccess(
                    usuarioId = user.usuarioId,
                    success = false,
                    input = input,
                    message = "Credenciales inválidas"
                )
                throw IllegalArgumentException("Credenciales inválidas")
            }

            val estado = user.estado ?: "Activo"

            if (!estado.equals("Activo", ignoreCase = true)) {
                registerAccess(
                    usuarioId = user.usuarioId,
                    success = false,
                    input = input,
                    message = "Usuario no activo"
                )
                throw IllegalArgumentException("Usuario no activo")
            }

            val now = nowIso()

            api.updateUsuario(
                usuarioId = "eq.${user.usuarioId}",
                body = mapOf(
                    "fechaultimoacceso" to now
                )
            )

            registerAccess(
                usuarioId = user.usuarioId,
                success = true,
                input = input,
                message = "Ingreso correcto"
            )

            val currentUser = CurrentUser(
                usuarioId = user.usuarioId,
                nombreUsuario = user.nombreUsuario ?: "",
                correoElectronico = user.correoElectronico ?: "",
                rolId = user.rolId ?: 1,
                estado = estado
            )

            AppSession.setUser(currentUser)

            currentUser
        }
    }

    private suspend fun findUser(input: String): UsuarioDto? {
        val cleanInput = input.trim()

        return if (cleanInput.contains("@")) {
            api.getUsuarioByCorreoElectronico("eq.$cleanInput").firstOrNull()
        } else {
            api.getUsuarioByNombreUsuario("eq.$cleanInput").firstOrNull()
        }
    }

    private suspend fun registerAccess(
        usuarioId: Int,
        success: Boolean,
        input: String,
        message: String
    ) {
        val metodoIngreso = if (input.contains("@")) {
            "CorreoElectronico"
        } else {
            "NombreUsuario"
        }

        runCatching {
            api.insertAccesoUsuario(
                mapOf(
                    "usuarioid" to usuarioId,
                    "fechaacceso" to nowIso(),
                    "exitoso" to success,
                    "metodoingreso" to metodoIngreso,
                    "mensajeresultado" to message
                )
            )
        }
    }
}