package com.example.exchangededivisas.api

import com.example.exchangededivisas.BuildConfig
import com.example.exchangededivisas.data.remote.GroqService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GroqApiClient {

    private const val BASE_URL = "https://api.groq.com/openai/v1/"

    val hasApiKey: Boolean
        get() = BuildConfig.GROQ_API_KEY.isNotBlank()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")

            if (BuildConfig.GROQ_API_KEY.isNotBlank()) {
                builder.addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
            }

            chain.proceed(builder.build())
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    val service: GroqService = retrofit.create(GroqService::class.java)
}
