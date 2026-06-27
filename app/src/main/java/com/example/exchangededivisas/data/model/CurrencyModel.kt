package com.example.exchangededivisas.data.model

data class CurrencyModel(
    val code: String,
    val name: String,
    val balance: Double,
    val isInternational: Boolean
)

object MockCurrencyData {
    val list = listOf(
        // Internacionales (10)
        CurrencyModel("USD", "Dólar Estadounidense", 2500.50, true),
        CurrencyModel("EUR", "Euro", 1200.00, true),
        CurrencyModel("GBP", "Libra Esterlina", 500.00, true),
        CurrencyModel("CHF", "Franco Suizo", 0.0, true),
        CurrencyModel("JPY", "Yen Japonés", 0.0, true),
        CurrencyModel("HKD", "Dólar de Hong Kong", 0.0, true),
        CurrencyModel("CAD", "Dólar Canadiense", 0.0, true),
        CurrencyModel("CNY", "Yuan Chino", 0.0, true),
        CurrencyModel("AUD", "Dólar Australiano", 0.0, true),
        CurrencyModel("RUB", "Rublo Ruso", 0.0, true),

        // Latinoamericanas (19)
        CurrencyModel("PEN", "Sol Peruano", 8500.00, false),
        CurrencyModel("BRL", "Real Brasileño", 350.00, false),
        CurrencyModel("MXN", "Peso Mexicano", 120.00, false),
        CurrencyModel("ARS", "Peso Argentino", 0.0, false),
        CurrencyModel("BOB", "Boliviano", 0.0, false),
        CurrencyModel("CLP", "Peso Chileno", 0.0, false),
        CurrencyModel("COP", "Peso Colombiano", 0.0, false),
        CurrencyModel("CRC", "Colón Costarricense", 0.0, false),
        CurrencyModel("CUP", "Peso Cubano", 0.0, false),
        CurrencyModel("GTQ", "Quetzal", 0.0, false),
        CurrencyModel("HNL", "Lempira", 0.0, false),
        CurrencyModel("NIO", "Córdoba", 0.0, false),
        CurrencyModel("PAB", "Balboa", 0.0, false),
        CurrencyModel("PYG", "Guaraní", 0.0, false),
        CurrencyModel("DOP", "Peso Dominicano", 0.0, false),
        CurrencyModel("UYU", "Peso Uruguayo", 0.0, false),
        CurrencyModel("HTG", "Gourde", 0.0, false),
        CurrencyModel("SRD", "Dólar Surinamés", 0.0, false),
        CurrencyModel("VES", "Bolívar Soberano", 0.0, false)
    ).sortedByDescending { it.balance } // Criterio: Ordenado de mayor a menor
}