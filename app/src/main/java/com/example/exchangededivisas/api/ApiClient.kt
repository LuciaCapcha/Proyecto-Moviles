package com.example.exchangededivisas.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // 1. Cambia esto por la URL de tu proyecto en Supabase (debe terminar en /rest/v1/)
    private const val BASE_URL = "https://azkosiumvyjnzxubpjju.supabase.co/rest/v1/"

    // 2. Coloca tu anon key aquí (puedes verla en Project Settings -> API)
    private const val API_KEY = "sb_publishable_uY-E0r1E9mLAsheVD_SSaw_iTQttwuG"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        // 3. Añadimos un interceptor para enviar la API Key en cada petición
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    fun getClient(): Retrofit {
        return retrofit
    }
}