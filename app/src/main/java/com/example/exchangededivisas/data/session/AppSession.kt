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

    private val _currentUser = MutableStateFlow(
        CurrentUser(
            usuarioId = 1,
            nombreUsuario = "usuario_demo",
            correoElectronico = "demo@ezchange.com",
            rolId = 1,
            estado = "Activo"
        )
    )

    val currentUser: StateFlow<CurrentUser> = _currentUser.asStateFlow()

    private val _walletRefreshTick = MutableStateFlow(0)
    val walletRefreshTick: StateFlow<Int> = _walletRefreshTick.asStateFlow()

    fun setUser(user: CurrentUser) {
        _currentUser.value = user
        notifyWalletChanged()
    }

    fun logout() {
        _currentUser.value = CurrentUser(
            usuarioId = 1,
            nombreUsuario = "usuario_demo",
            correoElectronico = "demo@ezchange.com",
            rolId = 1,
            estado = "Activo"
        )
        notifyWalletChanged()
    }

    fun notifyWalletChanged() {
        _walletRefreshTick.value = _walletRefreshTick.value + 1
    }
}