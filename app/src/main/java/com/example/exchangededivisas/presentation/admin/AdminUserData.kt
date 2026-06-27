package com.example.exchangededivisas.presentation.admin

import androidx.compose.runtime.mutableStateListOf
// Estructura de cada transacción del usuario
data class AdminUserTransaction(
    val fecha: String,
    val tipo: String, // "Depósito", "Retiro", "Compra", "Venta"
    val detalle: String,
    val monto: String
)

// Estructura principal del usuario
data class AdminUser(
    val id: Int,
    val nombre: String,
    val correo: String,
    var estaRestringido: Boolean,
    val saldos: Map<String, Double>, // Moneda -> Saldo
    val historial: List<AdminUserTransaction>
)


object MockAdminUsers {
    // Debe ser un 'mutableStateListOf' directo para que permita la modificación por index
    val lista = mutableStateListOf(
        AdminUser(
            id = 1,
            nombre = "Carlos Mendoza",
            correo = "carlos.mendoza@gmail.com",
            estaRestringido = false,
            saldos = mapOf("USD" to 1500.0, "PEN" to 5400.0),
            historial = emptyList()
        ),
        AdminUser(
            id = 2,
            nombre = "Diana Torres",
            correo = "diana.torres@outlook.com",
            estaRestringido = true,
            saldos = mapOf("USD" to 50.0),
            historial = emptyList()
        ),
        AdminUser(
            id = 3,
            nombre = "Gianfranco Reyna",
            correo = "gian.reyna@esan.edu.pe",
            estaRestringido = false,
            saldos = mapOf("USD" to 3200.50),
            historial = emptyList()
        ),
        AdminUser(
            id = 4,
            nombre = "Lucía Capcha",
            correo = "lucia@esan.edu.pe",
            estaRestringido = false,
            saldos = mapOf("EUR" to 1200.0),
            historial = emptyList()
        )
    )
}