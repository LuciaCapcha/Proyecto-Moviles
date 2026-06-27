package com.example.exchangededivisas.api

import com.example.exchangededivisas.models.CurrentPrice
import com.example.exchangededivisas.models.Order
import com.example.exchangededivisas.models.OrderBook
import com.example.exchangededivisas.models.Wallet
import retrofit2.Call
import retrofit2.http.*

interface OrderService {

    @POST("orders/create")
    fun createOrder(@Body order: Order): Call<Order>

    @GET("orders/current-price/{currencyPair}")
    fun getCurrentPrice(@Path("currencyPair") currencyPair: String): Call<CurrentPrice>

    @GET("wallet/{userId}")
    fun getWallet(@Path("userId") userId: String): Call<Wallet>

    @GET("orders/user/{userId}")
    fun getUserOrders(@Path("userId") userId: String): Call<List<Order>>

    @GET("orderbook/{currencyPair}")
    fun getOrderBook(@Path("currencyPair") currencyPair: String): Call<OrderBook>

    // WebSocket para actualizaciones en tiempo real (opcional, avanzado)
    @GET("orderbook/{currencyPair}/stream")
    fun subscribeOrderBook(@Path("currencyPair") currencyPair: String): Call<OrderBook>
}

