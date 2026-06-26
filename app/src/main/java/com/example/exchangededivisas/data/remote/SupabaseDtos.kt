package com.example.exchangededivisas.data.remote

import com.google.gson.annotations.SerializedName

data class UsuarioDto(
    @SerializedName("usuarioid") val usuarioId: Int,
    @SerializedName("rolid") val rolId: Int?,
    @SerializedName("paisid") val paisId: Int?,
    @SerializedName("nombreusuario") val nombreUsuario: String?,
    @SerializedName("correoelectronico") val correoElectronico: String?,
    @SerializedName("passwordhash") val passwordHash: String?,
    @SerializedName("temavisual") val temaVisual: String?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("fecharegistro") val fechaRegistro: String?,
    @SerializedName("fechaultimoacceso") val fechaUltimoAcceso: String?
)

data class MonedaDto(
    @SerializedName("monedaid") val monedaId: Int,
    @SerializedName("codigoiso") val codigoIso: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("activa") val activa: Boolean?
)

data class BilleteraDto(
    @SerializedName("billeteraid") val billeteraId: Int,
    @SerializedName("usuarioid") val usuarioId: Int,
    @SerializedName("fechacreacion") val fechaCreacion: String?
)

data class SaldoBilleteraDto(
    @SerializedName("saldoid") val saldoId: Int,
    @SerializedName("billeteraid") val billeteraId: Int,
    @SerializedName("monedaid") val monedaId: Int,
    @SerializedName("saldodisponible") val saldoDisponible: Double,
    @SerializedName("fechaactualizacion") val fechaActualizacion: String?
)

data class MetodoPagoDto(
    @SerializedName("metodopagoid") val metodoPagoId: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("comisionporcentaje") val comisionPorcentaje: Double?,
    @SerializedName("comisionfija") val comisionFija: Double?,
    @SerializedName("activo") val activo: Boolean?
)

data class DepositoDto(
    @SerializedName("depositoid") val depositoId: Int,
    @SerializedName("usuarioid") val usuarioId: Int,
    @SerializedName("monedaid") val monedaId: Int,
    @SerializedName("metodopagoid") val metodoPagoId: Int,
    @SerializedName("montodepositado") val montoDepositado: Double,
    @SerializedName("comisionaplicada") val comisionAplicada: Double,
    @SerializedName("totalpagado") val totalPagado: Double,
    @SerializedName("estado") val estado: String,
    @SerializedName("voucherurl") val voucherUrl: String?,
    @SerializedName("fechadeposito") val fechaDeposito: String?
)

data class ParMonedaDto(
    @SerializedName("parmonedaid") val parMonedaId: Int,
    @SerializedName("monedaorigenid") val monedaOrigenId: Int,
    @SerializedName("monedadestinoid") val monedaDestinoId: Int,
    @SerializedName("activo") val activo: Boolean?
)

data class OfertaVentaDto(
    @SerializedName("ofertaventaid") val ofertaVentaId: Int,
    @SerializedName("usuarioid") val usuarioId: Int,
    @SerializedName("parmonedaid") val parMonedaId: Int,
    @SerializedName("cantidadoriginal") val cantidadOriginal: Double,
    @SerializedName("cantidadvendida") val cantidadVendida: Double,
    @SerializedName("cantidadpendiente") val cantidadPendiente: Double,
    @SerializedName("preciounitario") val precioUnitario: Double,
    @SerializedName("totalesperado") val totalEsperado: Double,
    @SerializedName("totalrecibido") val totalRecibido: Double,
    @SerializedName("estado") val estado: String,
    @SerializedName("fechacreacion") val fechaCreacion: String?,
    @SerializedName("fechaactualizacion") val fechaActualizacion: String?,
    @SerializedName("fechacancelacion") val fechaCancelacion: String?,
    @SerializedName("ordencompraespejoid") val ordenCompraEspejoId: Int?
)

data class OrdenCompraDto(
    @SerializedName("ordencompraid") val ordenCompraId: Int,
    @SerializedName("usuarioid") val usuarioId: Int,
    @SerializedName("parmonedaid") val parMonedaId: Int,
    @SerializedName("cantidadoriginal") val cantidadOriginal: Double,
    @SerializedName("cantidadobtenida") val cantidadObtenida: Double,
    @SerializedName("cantidadpendiente") val cantidadPendiente: Double,
    @SerializedName("preciounitario") val precioUnitario: Double,
    @SerializedName("totalcomprometido") val totalComprometido: Double,
    @SerializedName("totalejecutado") val totalEjecutado: Double,
    @SerializedName("estado") val estado: String,
    @SerializedName("fechacreacion") val fechaCreacion: String?,
    @SerializedName("fechaactualizacion") val fechaActualizacion: String?,
    @SerializedName("fechacancelacion") val fechaCancelacion: String?
)

data class OperacionInmediataDto(
    @SerializedName("operacioninmediataid") val operacionInmediataId: Int,
    @SerializedName("usuarioid") val usuarioId: Int,
    @SerializedName("parmonedaid") val parMonedaId: Int,
    @SerializedName("tipooperacion") val tipoOperacion: String,
    @SerializedName("metodoejecucion") val metodoEjecucion: String,
    @SerializedName("cantidadsolicitada") val cantidadSolicitada: Double,
    @SerializedName("cantidadejecutada") val cantidadEjecutada: Double,
    @SerializedName("preciominimo") val precioMinimo: Double?,
    @SerializedName("preciomaximo") val precioMaximo: Double?,
    @SerializedName("preciopromedio") val precioPromedio: Double?,
    @SerializedName("totalpagado") val totalPagado: Double?,
    @SerializedName("totalrecibido") val totalRecibido: Double?,
    @SerializedName("estado") val estado: String,
    @SerializedName("fechaoperacion") val fechaOperacion: String?,
    @SerializedName("operacionpadreid") val operacionPadreId: Int?
)

data class CancelacionDto(
    @SerializedName("cancelacionid") val cancelacionId: Int,
    @SerializedName("usuarioid") val usuarioId: Int,
    @SerializedName("tipooperacion") val tipoOperacion: String,
    @SerializedName("ordencompraid") val ordenCompraId: Int?,
    @SerializedName("ofertaventaid") val ofertaVentaId: Int?,
    @SerializedName("parmonedaid") val parMonedaId: Int,
    @SerializedName("cantidadejecutada") val cantidadEjecutada: Double,
    @SerializedName("cantidadcancelada") val cantidadCancelada: Double,
    @SerializedName("montoreembolsado") val montoReembolsado: Double,
    @SerializedName("fechacancelacion") val fechaCancelacion: String?
)

data class HistorialDto(
    @SerializedName("historialid") val historialId: Int?
)

data class MovimientoDto(
    @SerializedName("movimientoid") val movimientoId: Int?
)

data class NotificacionDto(
    @SerializedName("notificacionid") val notificacionId: Int?
)

data class HistoricoPrecioParDto(
    @SerializedName("historicoprecioid") val historicoPrecioId: Int,
    @SerializedName("parmonedaid") val parMonedaId: Int,
    @SerializedName("mayorpreciocompra") val mayorPrecioCompra: Double?,
    @SerializedName("menorprecioventa") val menorPrecioVenta: Double?,
    @SerializedName("volumencompra") val volumenCompra: Double,
    @SerializedName("volumenventa") val volumenVenta: Double,
    @SerializedName("fecharegistro") val fechaRegistro: String?
)

data class EjecucionOrdenDto(
    @SerializedName("ejecucionid") val ejecucionId: Int,
    @SerializedName("parmonedaid") val parMonedaId: Int,
    @SerializedName("cantidadejecutada") val cantidadEjecutada: Double,
    @SerializedName("preciounitario") val precioUnitario: Double,
    @SerializedName("totaloperacion") val totalOperacion: Double,
    @SerializedName("fechaejecucion") val fechaEjecucion: String?
)