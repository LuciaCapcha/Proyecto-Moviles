package com.example.exchangededivisas.data.repository

import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.data.remote.UsuarioDto
import com.example.exchangededivisas.data.session.AppSession
import com.example.exchangededivisas.data.session.CurrentUser
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.security.MessageDigest

object AuthRepository {

    private val api = ApiClient.supabase

    private fun nowIso(): String {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
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

            val hashedPassword = sha256(rawPassword)
            val isPasswordValid = user.passwordHash == hashedPassword || user.passwordHash == rawPassword

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
            val canLogin = estado.equals("Activo", ignoreCase = true) ||
                    estado.equals("Restringido", ignoreCase = true)

            if (!canLogin) {
                registerAccess(
                    usuarioId = user.usuarioId,
                    success = false,
                    input = input,
                    message = "Usuario no habilitado"
                )
                throw IllegalArgumentException("Usuario no habilitado")
            }

            val now = nowIso()

            api.updateUsuario(
                usuarioId = "eq.${user.usuarioId}",
                body = mapOf("fechaultimoacceso" to now)
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

    suspend fun register(
        nombreUsuario: String,
        correo: String,
        password: String
    ): Result<CurrentUser> = runCatching {
        val user = nombreUsuario.trim()
        val email = correo.trim()
        val pass = password.trim()

        if (api.getUsuarioByCorreoElectronico("eq.$email").isNotEmpty()) {
            throw IllegalArgumentException("El correo ya está registrado")
        }
        if (api.getUsuarioByNombreUsuario("eq.$user").isNotEmpty()) {
            throw IllegalArgumentException("El nombre de usuario ya existe")
        }

        val rolId = (api.getRoles().firstOrNull { it["nombre"] == "USU" }
            ?: api.getRoles().firstOrNull())
            ?.get("rolid")?.let { (it as Number).toInt() }
            ?: throw IllegalStateException("No hay roles en la base de datos")

        val paisId = api.getPaises().firstOrNull()
            ?.get("paisid")?.let { (it as Number).toInt() }
            ?: throw IllegalStateException("No hay países en la base de datos")

        val nuevo = api.insertUsuario(
            mapOf(
                "rolid" to rolId,
                "paisid" to paisId,
                "nombreusuario" to user,
                "correoelectronico" to email,
                "passwordhash" to sha256(pass),
                "temavisual" to "Oscuro",
                "estado" to "Activo",
                "fecharegistro" to nowIso(),
                "fechaultimoacceso" to nowIso()
            )
        ).firstOrNull() ?: throw IllegalStateException("No se pudo crear el usuario")

        runCatching {
            api.insertBilletera(
                mapOf(
                    "usuarioid" to nuevo.usuarioId,
                    "fechacreacion" to nowIso()
                )
            )
        }

        val current = CurrentUser(
            usuarioId = nuevo.usuarioId,
            nombreUsuario = nuevo.nombreUsuario ?: user,
            correoElectronico = nuevo.correoElectronico ?: email,
            rolId = nuevo.rolId ?: rolId,
            estado = nuevo.estado ?: "Activo"
        )
        AppSession.setUser(current)
        current
    }
}
