package com.example.exchangededivisas.api

import com.example.exchangededivisas.data.remote.SupabaseService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "https://azkosiumvyjnzxubpjju.supabase.co/rest/v1/"
    private const val API_KEY = "sb_publishable_uY-E0r1E9mLAsheVD_SSaw_iTQttwuG"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS // Reducido para mejorar rendimiento
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    val supabase: SupabaseService = retrofit.create(SupabaseService::class.java)

    fun getClient(): Retrofit = retrofit
}