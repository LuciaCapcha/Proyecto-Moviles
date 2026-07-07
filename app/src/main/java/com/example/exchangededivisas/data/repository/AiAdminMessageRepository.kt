package com.example.exchangededivisas.data.repository

import com.example.exchangededivisas.api.GroqApiClient
import com.example.exchangededivisas.data.remote.GroqChatRequest
import com.example.exchangededivisas.data.remote.GroqMessage

object AiAdminMessageRepository {

    private const val MODEL = "llama-3.1-8b-instant"
    private const val MAX_CHARS = 300

    suspend fun generateRestrictionMessage(
        usuario: AdminUserUi,
        restrict: Boolean,
        draftMessage: String = ""
    ): Result<String> {
        return runCatching {
            val hasDraft = draftMessage.isNotBlank()

            if (!GroqApiClient.hasApiKey) {
                return@runCatching fallbackMessage(usuario, restrict, draftMessage)
            }

            val action = if (restrict) "restringir" else "habilitar"
            val status = if (restrict) "restringida" else "habilitada"
            val recentHistory = usuario.historial.take(10).joinToString("\n") { tx ->
                "- ${tx.fecha} | ${tx.tipo} | ${tx.estado} | ${tx.detalle}"
            }.ifBlank { "Sin historial reciente registrado." }

            val instruction = if (hasDraft) {
                """
                Mejora el mensaje escrito por el administrador. Conserva su intención, corrige redacción y tono, y no agregues acusaciones ni hechos no demostrados.
                Mensaje actual del administrador:
                $draftMessage
                """.trimIndent()
            } else {
                """
                Genera desde cero un mensaje breve para $action la cuenta del usuario.
                Usa el historial solo como contexto general; no inventes pruebas ni acuses fraude.
                """.trimIndent()
            }

            val prompt = """
                Eres asistente de un administrador de una app móvil de exchange de divisas.
                $instruction

                Reglas obligatorias:
                - Español formal y claro.
                - Máximo $MAX_CHARS caracteres.
                - 1 a 3 frases.
                - Tono preventivo, no acusatorio.
                - No menciones que usaste IA.
                - Devuelve solo el mensaje final, sin título, sin viñetas y sin comillas.

                Usuario: ${usuario.nombre}
                Correo: ${usuario.correo}
                Estado actual: ${usuario.estado}
                Acción administrativa: cuenta $status.
                Saldos: ${usuario.saldos.entries.joinToString { "${it.key}: %.2f".format(it.value) }.ifBlank { "Sin saldos" }}
                Últimas 10 operaciones:
                $recentHistory
            """.trimIndent()

            val response = GroqApiClient.service.createChatCompletion(
                GroqChatRequest(
                    model = MODEL,
                    temperature = 0.45,
                    maxTokens = 120,
                    messages = listOf(
                        GroqMessage(
                            role = "system",
                            content = "Redactas mensajes administrativos concisos para una plataforma financiera. No inventas evidencia, no acusas fraude y respetas límites de longitud."
                        ),
                        GroqMessage(
                            role = "user",
                            content = prompt
                        )
                    )
                )
            )

            response.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?.trim()
                ?.replace("\n", " ")
                ?.take(MAX_CHARS)
                ?.takeIf { it.isNotBlank() }
                ?: fallbackMessage(usuario, restrict, draftMessage)
        }
    }

    private fun fallbackMessage(
        usuario: AdminUserUi,
        restrict: Boolean,
        draftMessage: String = ""
    ): String {
        if (draftMessage.isNotBlank()) {
            return draftMessage.trim().replace("\n", " ").take(MAX_CHARS)
        }

        return if (restrict) {
            "Hola ${usuario.nombre}, por prevención hemos restringido temporalmente tu cuenta mientras revisamos tu actividad reciente. Podrás retirar tus fondos disponibles durante este periodo."
        } else {
            "Hola ${usuario.nombre}, tu cuenta ha sido habilitada nuevamente luego de la revisión administrativa. Ya puedes volver a operar con normalidad."
        }.take(MAX_CHARS)
    }
}
