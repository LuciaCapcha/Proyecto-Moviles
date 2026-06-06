package com.example.exchangededivisas.models

import java.io.Serializable

data class Wallet(
    val walletId: String = "",
    val userId: String = "",
    val currency: String = "",
    val balance: Double = 0.0,
    val committedAmount: Double = 0.0
) : Serializable

