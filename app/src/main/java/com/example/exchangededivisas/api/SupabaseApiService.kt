package com.example.exchangededivisas.api

import com.example.exchangededivisas.data.remote.HistoricoPrecioParDto
import com.example.exchangededivisas.data.remote.HistorialTransaccionDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SupabaseApiService {

    @GET("historicopreciospar")
    suspend fun getHistoricoPrecios(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Query("parmonedaid") parId: String, // ej: "eq.1"
        @Query("fecharegistro") fechaGte: String? = null, // ej: "gte.2026-01-01"
        @Query("order") order: String = "fecharegistro.asc"
    ): List<HistoricoPrecioParDto>

    @GET("historialtransacciones")
    suspend fun getHistorialUsuario(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Query("usuarioid") usuarioId: String // ej: "eq.10"
    ): List<HistorialTransaccionDto>
}
