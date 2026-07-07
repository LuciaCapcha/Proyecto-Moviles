package com.example.exchangededivisas.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object EmailDispatchRepository {

    private const val FUNCTION_URL =
        "https://azkosiumvyjnzxubpjju.supabase.co/functions/v1/send-pending-emails"

    private val client = OkHttpClient()

    suspend fun flushPendingEmails(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(FUNCTION_URL)
                    .post(ByteArray(0).toRequestBody(null))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("No se pudieron enviar los correos pendientes. Código HTTP: ${response.code}")
                    }
                }
            }
        }
    }
}
