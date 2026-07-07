package com.example.exchangededivisas.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CurrentUser(
    val usuarioId: Int,
    val nombreUsuario: String,
    val correoElectronico: String,
    val rolId: Int,
    val estado: String
)

object AppSession {

    private val emptyUser = CurrentUser(
        usuarioId = 0,
        nombreUsuario = "",
        correoElectronico = "",
        rolId = 0,
        estado = "Sin sesión"
    )

    private val _currentUser = MutableStateFlow(emptyUser)

    val currentUser: StateFlow<CurrentUser> = _currentUser.asStateFlow()

    private val _walletRefreshTick = MutableStateFlow(0)
    val walletRefreshTick: StateFlow<Int> = _walletRefreshTick.asStateFlow()

    fun setUser(user: CurrentUser) {
        _currentUser.value = user
        notifyWalletChanged()
    }

    fun logout() {
        _currentUser.value = emptyUser
        notifyWalletChanged()
    }

    fun notifyWalletChanged() {
        _walletRefreshTick.value = _walletRefreshTick.value + 1
    }
}