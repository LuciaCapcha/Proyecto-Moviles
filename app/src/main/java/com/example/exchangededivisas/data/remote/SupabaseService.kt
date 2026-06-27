package com.example.exchangededivisas.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseService {

    @GET("usuarios")
    suspend fun getUsuarioById(
        @Query("usuarioid") usuarioId: String,
        @Query("limit") limit: Int = 1
    ): List<UsuarioDto>

    @GET("usuarios")
    suspend fun getUsuarioByNombreUsuario(
        @Query("nombreusuario") nombreUsuario: String,
        @Query("limit") limit: Int = 1
    ): List<UsuarioDto>

    @GET("usuarios")
    suspend fun getUsuarioByCorreoElectronico(
        @Query("correoelectronico") correoElectronico: String,
        @Query("limit") limit: Int = 1
    ): List<UsuarioDto>

    @PATCH("usuarios")
    @Headers("Prefer: return=representation")
    suspend fun updateUsuario(
        @Query("usuarioid") usuarioId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<UsuarioDto>

    @POST("accesosusuario")
    @Headers("Prefer: return=representation")
    suspend fun insertAccesoUsuario(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<Map<String, Any?>>

    @GET("monedas")
    suspend fun getMonedas(
        @Query("activa") activa: String = "eq.true",
        @Query("order") order: String = "codigoiso.asc"
    ): List<MonedaDto>

    @GET("monedas")
    suspend fun getMonedaByCode(
        @Query("codigoiso") codigoIso: String,
        @Query("limit") limit: Int = 1
    ): List<MonedaDto>

    @GET("monedas")
    suspend fun getMonedaById(
        @Query("monedaid") monedaId: String,
        @Query("limit") limit: Int = 1
    ): List<MonedaDto>

    @GET("billeteras")
    suspend fun getBilleteraByUser(
        @Query("usuarioid") usuarioId: String,
        @Query("limit") limit: Int = 1
    ): List<BilleteraDto>

    @POST("billeteras")
    @Headers("Prefer: return=representation")
    suspend fun insertBilletera(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<BilleteraDto>

    @GET("saldosbilletera")
    suspend fun getSaldosByWallet(
        @Query("billeteraid") billeteraId: String
    ): List<SaldoBilleteraDto>

    @GET("saldosbilletera")
    suspend fun getSaldo(
        @Query("billeteraid") billeteraId: String,
        @Query("monedaid") monedaId: String,
        @Query("limit") limit: Int = 1
    ): List<SaldoBilleteraDto>

    @POST("saldosbilletera")
    @Headers("Prefer: return=representation")
    suspend fun insertSaldo(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<SaldoBilleteraDto>

    @PATCH("saldosbilletera")
    @Headers("Prefer: return=representation")
    suspend fun updateSaldo(
        @Query("saldoid") saldoId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<SaldoBilleteraDto>

    @GET("metodospago")
    suspend fun getMetodosPago(
        @Query("activo") activo: String = "eq.true",
        @Query("order") order: String = "nombre.asc"
    ): List<MetodoPagoDto>

    @GET("metodospago")
    suspend fun getMetodoPagoByName(
        @Query("nombre") nombre: String,
        @Query("limit") limit: Int = 1
    ): List<MetodoPagoDto>

    @POST("depositos")
    @Headers("Prefer: return=representation")
    suspend fun insertDeposito(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<DepositoDto>

    @POST("retiros")
    @Headers("Prefer: return=representation")
    suspend fun insertRetiro(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<RetiroDto>
    @POST("movimientosbilletera")
    @Headers("Prefer: return=representation")
    suspend fun insertMovimiento(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<MovimientoDto>

    @GET("paresmoneda")
    suspend fun getParMoneda(
        @Query("monedaorigenid") monedaOrigenId: String,
        @Query("monedadestinoid") monedaDestinoId: String,
        @Query("activo") activo: String = "eq.true",
        @Query("limit") limit: Int = 1
    ): List<ParMonedaDto>

    @GET("paresmoneda")
    suspend fun getParMonedaById(
        @Query("parmonedaid") parMonedaId: String,
        @Query("limit") limit: Int = 1
    ): List<ParMonedaDto>

    @GET("ofertasventa")
    suspend fun getOfertasVentaByPair(
        @Query("parmonedaid") parMonedaId: String,
        @Query("order") order: String = "preciounitario.asc,fechacreacion.asc"
    ): List<OfertaVentaDto>

    @GET("ofertasventa")
    suspend fun getOfertasVentaByUser(
        @Query("usuarioid") usuarioId: String,
        @Query("order") order: String = "fechacreacion.desc"
    ): List<OfertaVentaDto>

    @PATCH("ofertasventa")
    @Headers("Prefer: return=representation")
    suspend fun updateOfertaVenta(
        @Query("ofertaventaid") ofertaVentaId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<OfertaVentaDto>

    @POST("ofertasventa")
    @Headers("Prefer: return=representation")
    suspend fun insertOfertaVenta(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<OfertaVentaDto>
    @GET("ordenescompra")
    suspend fun getOrdenesCompraByUser(
        @Query("usuarioid") usuarioId: String,
        @Query("order") order: String = "fechacreacion.desc"
    ): List<OrdenCompraDto>
    @GET("ordenescompra")
    suspend fun getOrdenesCompraByPair(
        @Query("parmonedaid") parMonedaId: String,
        @Query("estado") estado: String = "in.(Activa,Parcialmente ejecutada)",
        @Query("order") order: String = "preciounitario.desc,fechacreacion.asc"
    ): List<OrdenCompraDto>
    @PATCH("ordenescompra")
    @Headers("Prefer: return=representation")
    suspend fun updateOrdenCompra(
        @Query("ordencompraid") ordenCompraId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<OrdenCompraDto>

    @POST("operacionesinmediatas")
    @Headers("Prefer: return=representation")
    suspend fun insertOperacionInmediata(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<OperacionInmediataDto>

    @POST("cancelacionesordenoferta")
    @Headers("Prefer: return=representation")
    suspend fun insertCancelacion(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<CancelacionDto>

    @POST("historialtransacciones")
    @Headers("Prefer: return=representation")
    suspend fun insertHistorial(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<HistorialDto>

    @POST("notificacionescorreo")
    @Headers("Prefer: return=representation")
    suspend fun insertNotificacionCorreo(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<NotificacionDto>

    @GET("historicopreciospar")
    suspend fun getHistoricoByPar(
        @Query("parmonedaid") parMonedaId: String,
        @Query("order") order: String = "fecharegistro.desc",
        @Query("limit") limit: Int = 50
    ): List<HistoricoPrecioParDto>

    @GET("paresmoneda")
    suspend fun getAllParesMoneda(
        @Query("activo") activo: String = "eq.true"
    ): List<ParMonedaDto>

    @GET("ejecucionesorden")
    suspend fun getEjecucionesByPar(
        @Query("parmonedaid") parMonedaId: String,
        @Query("order") order: String = "fechaejecucion.desc",
        @Query("limit") limit: Int = 10
    ): List<EjecucionOrdenDto>

    @GET("usuarios")
    suspend fun getAllUsuarios(
        @Query("order") order: String = "fecharegistro.desc"
    ): List<UsuarioDto>

    @GET("usuarios")
    suspend fun getUsuariosByEstado(
        @Query("estado") estado: String,
        @Query("order") order: String = "fecharegistro.desc"
    ): List<UsuarioDto>

    @PATCH("usuarios")
    @Headers("Prefer: return=representation")
    suspend fun updateUsuarioEstado(
        @Query("usuarioid") usuarioId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<UsuarioDto>

    @GET("historialtransacciones")
    suspend fun getHistorialByUsuario(
        @Query("usuarioid") usuarioId: String,
        @Query("order") order: String = "fechahora.desc",
        @Query("limit") limit: Int = 20
    ): List<HistorialTransaccionDto>

    @POST("restriccionesusuario")
    @Headers("Prefer: return=representation")
    suspend fun insertRestriccion(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<Map<String, Any?>>

    @POST("auditoriaadministrativa")
    @Headers("Prefer: return=representation")
    suspend fun insertAuditoria(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): List<Map<String, Any?>>

    @GET("ordenescompra")
    suspend fun countOrdenesActivas(
        @Query("estado") estado: String = "in.(Activa,Parcialmente ejecutada)",
        @Query("select") select: String = "ordencompraid"
    ): List<Map<String, Any?>>

    @GET("ofertasventa")
    suspend fun countOfertasActivas(
        @Query("estado") estado: String = "in.(Activa,Parcialmente ejecutada)",
        @Query("select") select: String = "ofertaventaid"
    ): List<Map<String, Any?>>

    @GET("ejecucionesorden")
    suspend fun getEjecucionesHoy(
        @Query("fechaejecucion") fecha: String,
        @Query("select") select: String = "ejecucionid,totaloperacion"
    ): List<Map<String, Any?>>

}