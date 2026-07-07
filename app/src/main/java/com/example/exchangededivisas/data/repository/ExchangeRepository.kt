package com.example.exchangededivisas.data.repository

import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.data.model.InstantTransaction
import com.example.exchangededivisas.data.model.OrderOrOffer
import com.example.exchangededivisas.data.model.TransactionType
import com.example.exchangededivisas.data.notification.NativeNotificationBus
import com.example.exchangededivisas.data.remote.HistorialTransaccionDto
import com.example.exchangededivisas.data.remote.OperacionInmediataDto
import com.example.exchangededivisas.data.remote.OrdenCompraDto
import java.time.LocalDateTime
import java.time.ZoneOffset
import com.example.exchangededivisas.data.remote.BilleteraDto
import com.example.exchangededivisas.data.remote.MetodoPagoDto
import com.example.exchangededivisas.data.remote.MonedaDto
import com.example.exchangededivisas.data.remote.OfertaVentaDto
import com.example.exchangededivisas.data.remote.HistoricoPrecioParDto
import com.example.exchangededivisas.data.remote.ParMonedaDto
import com.example.exchangededivisas.data.remote.SaldoBilleteraDto
import com.example.exchangededivisas.data.session.AppSession
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

data class WalletCurrencyUi(
    val monedaId: Int,
    val code: String,
    val name: String,
    val balance: Double,
    val isInternational: Boolean
)

data class PaymentMethodUi(
    val metodoPagoId: Int,
    val nombre: String,
    val comisionPorcentaje: Double,
    val comisionFija: Double
)

data class InstantBuyPreview(
    val pairCode: String,
    val fromCurrency: String,
    val toCurrency: String,
    val requestedAmount: Double,
    val coveredAmount: Double,
    val totalToPay: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val avgPrice: Double,
    val availableBalance: Double,
    val hasLiquidity: Boolean,
    val hasEnoughBalance: Boolean
)

data class InstantBuyReceipt(
    val operationId: Int,
    val boughtAmount: Double,
    val paidTotal: Double,
    val fromCurrency: String,
    val toCurrency: String
)

enum class ActiveTradeType {
    BUY_ORDER,
    SELL_OFFER
}

data class ActiveTradeUi(
    val id: Int,
    val type: ActiveTradeType,
    val pairId: Int,
    val pairCode: String,
    val dateText: String,
    val unitPrice: Double,
    val originalQuantity: Double,
    val originalTotal: Double,
    val executedQuantity: Double,
    val executedTotal: Double,
    val remainingQuantity: Double,
    val remainingTotal: Double,
    val refundAmount: Double,
    val refundCurrency: String
)

data class HistoryDataUi(
    val buyOrders: List<OrderOrOffer>,
    val sellOffers: List<OrderOrOffer>,
    val instantBuys: List<InstantTransaction>,
    val instantSells: List<InstantTransaction>
)

private data class PlannedOfferExecution(
    val offer: OfertaVentaDto,
    val amountTaken: Double,
    val subtotal: Double
)

private data class PlannedBuyOrderExecution(
    val order: OrdenCompraDto,
    val amountTaken: Double,
    val subtotal: Double
)

object ExchangeRepository {

    private val api = ApiClient.supabase

    private suspend fun notifyNative(
        recipientUserId: Int,
        title: String,
        body: String,
        notificationId: Int? = null
    ) {
        runCatching {
            NativeNotificationBus.emitForUser(
                recipientUserId = recipientUserId,
                title = title,
                body = body,
                notificationId = notificationId
            )
        }
    }

    private suspend fun dispatchPendingEmails() {
        runCatching {
            EmailDispatchRepository.flushPendingEmails()
        }
    }

    private const val STATIC_CACHE_MS = 10 * 60 * 1000L
    private const val MARKET_CACHE_MS = 8 * 1000L

    private data class TimedCache<T>(
        val value: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isFresh(ttlMs: Long): Boolean = System.currentTimeMillis() - timestamp <= ttlMs
    }

    private var cachedMonedas: TimedCache<Map<Int, MonedaDto>>? = null
    private var cachedPares: TimedCache<List<ParMonedaDto>>? = null
    private var cachedAllOrders: TimedCache<List<OrdenCompraDto>>? = null
    private var cachedAllOffers: TimedCache<List<OfertaVentaDto>>? = null
    private var cachedAllPairsUi: TimedCache<List<PairUi>>? = null
    private val chartCache = mutableMapOf<Int, TimedCache<com.example.exchangededivisas.data.model.CurrencyPairChartData>>()

    private suspend fun getMonedasCached(forceRefresh: Boolean = false): Map<Int, MonedaDto> {
        val existing = cachedMonedas
        if (!forceRefresh && existing != null && existing.isFresh(STATIC_CACHE_MS)) return existing.value
        return api.getMonedas().associateBy { it.monedaId }.also { cachedMonedas = TimedCache(it) }
    }

    private suspend fun getParesCached(forceRefresh: Boolean = false): List<ParMonedaDto> {
        val existing = cachedPares
        if (!forceRefresh && existing != null && existing.isFresh(STATIC_CACHE_MS)) return existing.value
        return api.getAllParesMoneda().also { cachedPares = TimedCache(it) }
    }

    private suspend fun getAllOrdersCached(forceRefresh: Boolean = false): List<OrdenCompraDto> {
        val existing = cachedAllOrders
        if (!forceRefresh && existing != null && existing.isFresh(MARKET_CACHE_MS)) return existing.value
        return api.getAllOrdenesCompraActivas()
            .filter { isActiveStatus(it.estado) && it.cantidadPendiente > 0.0 }
            .also { cachedAllOrders = TimedCache(it) }
    }

    private suspend fun getAllOffersCached(forceRefresh: Boolean = false): List<OfertaVentaDto> {
        val existing = cachedAllOffers
        if (!forceRefresh && existing != null && existing.isFresh(MARKET_CACHE_MS)) return existing.value
        return api.getAllOfertasActivas()
            .filter { isActiveStatus(it.estado) && it.cantidadPendiente > 0.0 }
            .also { cachedAllOffers = TimedCache(it) }
    }

    private fun invalidateMarketCache() {
        cachedAllOrders = null
        cachedAllOffers = null
        cachedAllPairsUi = null
        chartCache.clear()
    }

    suspend fun obtenerHistoricoGrafico(parId: Int, rangoTiempo: String): List<HistoricoPrecioParDto> {
        val now = ZonedDateTime.now()
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        val startDateTime = when (rangoTiempo) {
            "Último día" -> now.minusDays(1)
            "Última semana" -> now.minusWeeks(1)
            "Último mes" -> now.minusMonths(1)
            "Último año" -> now.minusYears(1)
            "Tiempo total" -> null
            else -> null
        }

        val fechaFiltro = startDateTime?.let { "gte.${it.format(formatter)}" }

        return api.getHistoricoByPar(
            parMonedaId = "eq.$parId",
            fechaGte = fechaFiltro
        )
    }

    private fun nowIso(): String {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun cleanCode(code: String): String {
        return code.trim().uppercase()
    }

    private fun parsePairCode(code: String): Pair<String, String> {
        val clean = code.replace("_", "/").trim().uppercase()
        val parts = clean.split("/")
        val originCode = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "PEN"
        val destinationCode = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "USD"
        return originCode to destinationCode
    }

    private fun isActiveStatus(status: String): Boolean {
        val cleanStatus = status.trim()
        return cleanStatus.equals("Activa", ignoreCase = true) ||
                cleanStatus.equals("Parcialmente ejecutada", ignoreCase = true)
    }

    private suspend fun ensureUserCanOperate(usuarioId: Int, actionName: String) {
        val user = api.getUsuarioById("eq.$usuarioId").firstOrNull()
            ?: error("No se encontró el usuario.")

        if (user.estado?.equals("Restringido", ignoreCase = true) == true) {
            val reason = getActiveRestrictionReason(usuarioId).getOrNull()
            val suffix = if (!reason.isNullOrBlank()) " Motivo: $reason" else ""
            error("Usuario restringido: no puede $actionName.$suffix")
        }
    }

    suspend fun getActiveRestrictionReason(usuarioId: Int): Result<String?> {
        return runCatching {
            api.getRestriccionesActivasByUsuario("eq.$usuarioId")
                .firstOrNull()
                ?.mensaje
        }
    }

    suspend fun getCurrencies(): List<WalletCurrencyUi> {
        return api.getMonedas()
            .map {
                WalletCurrencyUi(
                    monedaId = it.monedaId,
                    code = it.codigoIso.trim(),
                    name = it.nombre,
                    balance = 0.0,
                    isInternational = it.tipo?.contains("Internacional", ignoreCase = true) == true
                )
            }
    }

    suspend fun getPaymentMethods(): List<PaymentMethodUi> {
        return api.getMetodosPago().map {
            PaymentMethodUi(
                metodoPagoId = it.metodoPagoId,
                nombre = it.nombre,
                comisionPorcentaje = it.comisionPorcentaje ?: 0.0,
                comisionFija = it.comisionFija ?: 0.0
            )
        }
    }

    suspend fun loadWallet(usuarioId: Int): List<WalletCurrencyUi> {
        return try {
            val wallet = getOrCreateWallet(usuarioId)

            val currencies = api.getMonedas()
            val balances = api.getSaldosByWallet("eq.${wallet.billeteraId}")
                .associateBy { it.monedaId }

            currencies.map { currency ->
                val balance = balances[currency.monedaId]?.saldoDisponible ?: 0.0

                WalletCurrencyUi(
                    monedaId = currency.monedaId,
                    code = currency.codigoIso.trim(),
                    name = currency.nombre,
                    balance = balance,
                    isInternational = currency.tipo?.contains("Internacional", ignoreCase = true) == true
                )
            }.sortedByDescending { it.balance }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun makeDeposit(
        usuarioId: Int,
        currencyCode: String,
        paymentMethodName: String,
        amount: Double
    ): Result<String> {
        return runCatching {
            require(amount > 0.0) { "Monto inválido" }
            ensureUserCanOperate(usuarioId, "depositar dinero")

            val moneda = getCurrencyByCode(currencyCode)
            val metodo = getPaymentMethodByName(paymentMethodName)

            val commission = amount * ((metodo.comisionPorcentaje ?: 0.0) / 100.0) +
                    (metodo.comisionFija ?: 0.0)

            val totalPaid = amount + commission
            val now = nowIso()

            val deposito = api.insertDeposito(
                mapOf(
                    "usuarioid" to usuarioId,
                    "monedaid" to moneda.monedaId,
                    "metodopagoid" to metodo.metodoPagoId,
                    "montodepositado" to amount,
                    "comisionaplicada" to commission,
                    "totalpagado" to totalPaid,
                    "estado" to "Completada",
                    "voucherurl" to null,
                    "fechadeposito" to now
                )
            ).first()

            val (before, after) = addBalance(
                usuarioId = usuarioId,
                monedaId = moneda.monedaId,
                delta = amount
            )

            insertWalletMovement(
                usuarioId = usuarioId,
                monedaId = moneda.monedaId,
                tipoMovimiento = "Deposito",
                monto = amount,
                saldoAnterior = before,
                saldoPosterior = after,
                referenciaTipo = "depositos",
                referenciaId = deposito.depositoId,
                fecha = now
            )

            api.insertHistorial(
                mapOf(
                    "usuarioid" to usuarioId,
                    "tipooperacion" to "Deposito",
                    "referenciaid" to deposito.depositoId,
                    "parmonedaid" to null,
                    "monedaid" to moneda.monedaId,
                    "fechahora" to now,
                    "estado" to "Completada",
                    "metodoejecucion" to metodo.nombre
                )
            )

            val user = api.getUsuarioById("eq.$usuarioId").firstOrNull()
            val correo = user?.correoElectronico ?: "correo@pendiente.com"

            val asuntoDeposito = "Voucher de depósito Ezchange"
            val cuerpoDeposito = "Se registró un depósito de %.2f %s. Total pagado: %.2f. Comisión: %.2f."
                .format(amount, moneda.codigoIso.trim(), totalPaid, commission)

            val depositoNotificacion = api.insertNotificacionCorreo(
                mapOf(
                    "usuarioid" to usuarioId,
                    "tiponotificacionid" to null,
                    "correodestino" to correo,
                    "tipoevento" to "VOUCHER_DEPOSITO",
                    "asunto" to asuntoDeposito,
                    "cuerpo" to cuerpoDeposito,
                    "estadoenvio" to "Pendiente",
                    "fechacreacion" to now,
                    "fechaenvio" to null,
                    "referenciatipo" to "depositos",
                    "referenciaid" to deposito.depositoId
                )
            ).firstOrNull()

            notifyNative(usuarioId, asuntoDeposito, cuerpoDeposito, depositoNotificacion?.notificacionId)
            dispatchPendingEmails()

            AppSession.notifyWalletChanged()

            "Depósito registrado. Se generó una notificación de voucher pendiente de envío."
        }
    }

    suspend fun previewInstantBuy(
        usuarioId: Int,
        pairCode: String,
        amount: Double
    ): Result<InstantBuyPreview> {
        return runCatching {
            require(amount > 0.0) { "Valor inválido" }

            val (fromCode, toCode) = parsePairCode(pairCode)
            val fromCurrency = getCurrencyByCode(fromCode)
            val toCurrency = getCurrencyByCode(toCode)
            val pair = getPairOrThrow(fromCurrency.monedaId, toCurrency.monedaId)

            val offers = getActiveSellOffers(pairCode).getOrThrow()
            val planned = planSellOfferUiExecutions(offers, amount)

            val covered = planned.sumOf { it.second }
            val total = planned.sumOf { (offer, taken) -> taken * offer.price }
            val prices = planned.map { it.first.price }

            val availableBalance = getAvailableBalance(usuarioId, fromCurrency.monedaId)

            InstantBuyPreview(
                pairCode = "${fromCurrency.codigoIso.trim()}_${toCurrency.codigoIso.trim()}",
                fromCurrency = fromCurrency.codigoIso.trim(),
                toCurrency = toCurrency.codigoIso.trim(),
                requestedAmount = amount,
                coveredAmount = covered,
                totalToPay = total,
                minPrice = prices.minOrNull() ?: 0.0,
                maxPrice = prices.maxOrNull() ?: 0.0,
                avgPrice = if (covered > 0.0) total / covered else 0.0,
                availableBalance = availableBalance,
                hasLiquidity = covered >= amount,
                hasEnoughBalance = availableBalance >= total
            )
        }
    }

    suspend fun executeInstantBuy(
        usuarioId: Int,
        pairCode: String,
        amount: Double
    ): Result<InstantBuyReceipt> {
        return runCatching {
            require(amount > 0.0) { "Valor inválido" }
            ensureUserCanOperate(usuarioId, "comprar inmediatamente")

            val (fromCode, toCode) = parsePairCode(pairCode)
            val fromCurrency = getCurrencyByCode(fromCode)
            val toCurrency = getCurrencyByCode(toCode)
            val pair = getPairOrThrow(fromCurrency.monedaId, toCurrency.monedaId)

            val result = api.ejecutarCompraInmediataSegura(
                mapOf(
                    "p_usuarioid" to usuarioId,
                    "p_parmonedaid" to pair.parMonedaId,
                    "p_cantidad" to amount
                )
            )

            val operationId = result.operacionId
                ?: error(result.mensaje ?: "No se pudo ejecutar la compra inmediata.")

            invalidateMarketCache()
            AppSession.notifyWalletChanged()

            notifyNative(
                usuarioId,
                "Compra inmediata completada",
                "Compraste %.4f %s por %.4f %s.".format(
                    result.cantidadComprada ?: amount,
                    result.monedaDestino ?: toCurrency.codigoIso.trim(),
                    result.totalPagado ?: 0.0,
                    result.monedaOrigen ?: fromCurrency.codigoIso.trim()
                )
            )
            dispatchPendingEmails()

            InstantBuyReceipt(
                operationId = operationId,
                boughtAmount = result.cantidadComprada ?: amount,
                paidTotal = result.totalPagado ?: 0.0,
                fromCurrency = result.monedaOrigen ?: fromCurrency.codigoIso.trim(),
                toCurrency = result.monedaDestino ?: toCurrency.codigoIso.trim()
            )
        }
    }

    suspend fun loadActiveTransactions(usuarioId: Int): Result<List<ActiveTradeUi>> {
        return runCatching {
            val buyOrders = api.getOrdenesCompraByUser("eq.$usuarioId")
                .filter { isActiveStatus(it.estado) }
                .mapNotNull { order ->
                    val pair = api.getParMonedaById("eq.${order.parMonedaId}").firstOrNull()
                        ?: return@mapNotNull null

                    val from = api.getMonedaById("eq.${pair.monedaOrigenId}")
                        .firstOrNull()
                        ?.codigoIso
                        ?.trim()
                        ?: "?"

                    val to = api.getMonedaById("eq.${pair.monedaDestinoId}")
                        .firstOrNull()
                        ?.codigoIso
                        ?.trim()
                        ?: "?"

                    val remainingTotal = max(
                        0.0,
                        order.totalComprometido - order.totalEjecutado
                    )

                    ActiveTradeUi(
                        id = order.ordenCompraId,
                        type = ActiveTradeType.BUY_ORDER,
                        pairId = order.parMonedaId,
                        pairCode = "$from → $to",
                        dateText = formatDate(order.fechaCreacion),
                        unitPrice = order.precioUnitario,
                        originalQuantity = order.cantidadOriginal,
                        originalTotal = order.totalComprometido,
                        executedQuantity = order.cantidadObtenida,
                        executedTotal = order.totalEjecutado,
                        remainingQuantity = order.cantidadPendiente,
                        remainingTotal = remainingTotal,
                        refundAmount = remainingTotal,
                        refundCurrency = from
                    )
                }

            val sellOffers = api.getOfertasVentaByUser("eq.$usuarioId")
                .filter { isActiveStatus(it.estado) }
                .mapNotNull { offer ->
                    val pair = api.getParMonedaById("eq.${offer.parMonedaId}").firstOrNull()
                        ?: return@mapNotNull null

                    val from = api.getMonedaById("eq.${pair.monedaOrigenId}")
                        .firstOrNull()
                        ?.codigoIso
                        ?.trim()
                        ?: "?"

                    val to = api.getMonedaById("eq.${pair.monedaDestinoId}")
                        .firstOrNull()
                        ?.codigoIso
                        ?.trim()
                        ?: "?"

                    val remainingTotal = offer.cantidadPendiente * offer.precioUnitario

                    ActiveTradeUi(
                        id = offer.ofertaVentaId,
                        type = ActiveTradeType.SELL_OFFER,
                        pairId = offer.parMonedaId,
                        pairCode = "$from → $to",
                        dateText = formatDate(offer.fechaCreacion),
                        unitPrice = offer.precioUnitario,
                        originalQuantity = offer.cantidadOriginal,
                        originalTotal = offer.totalEsperado,
                        executedQuantity = offer.cantidadVendida,
                        executedTotal = offer.totalRecibido,
                        remainingQuantity = offer.cantidadPendiente,
                        remainingTotal = remainingTotal,
                        refundAmount = offer.cantidadPendiente,
                        refundCurrency = to
                    )
                }

            (buyOrders + sellOffers).sortedByDescending { it.dateText }
        }
    }

    suspend fun cancelActiveTransaction(
        usuarioId: Int,
        item: ActiveTradeUi
    ): Result<String> {
        return runCatching {
            val now = nowIso()

            val pair = api.getParMonedaById("eq.${item.pairId}").firstOrNull()
                ?: error("No se encontró el par de moneda.")

            val refundCurrencyId = if (item.type == ActiveTradeType.BUY_ORDER) {
                pair.monedaOrigenId
            } else {
                pair.monedaDestinoId
            }

            val (before, after) = addBalance(
                usuarioId = usuarioId,
                monedaId = refundCurrencyId,
                delta = item.refundAmount
            )

            if (item.type == ActiveTradeType.BUY_ORDER) {
                api.updateOrdenCompra(
                    ordenCompraId = "eq.${item.id}",
                    body = mapOf(
                        "estado" to "Cancelada",
                        "fechaactualizacion" to now,
                        "fechacancelacion" to now
                    )
                )
            } else {
                api.updateOfertaVenta(
                    ofertaVentaId = "eq.${item.id}",
                    body = mapOf(
                        "estado" to "Cancelada",
                        "fechaactualizacion" to now,
                        "fechacancelacion" to now
                    )
                )
            }

            val cancelacion = api.insertCancelacion(
                mapOf(
                    "usuarioid" to usuarioId,
                    "tipooperacion" to if (item.type == ActiveTradeType.BUY_ORDER) {
                        "Orden de compra"
                    } else {
                        "Oferta de venta"
                    },
                    "ordencompraid" to if (item.type == ActiveTradeType.BUY_ORDER) item.id else null,
                    "ofertaventaid" to if (item.type == ActiveTradeType.SELL_OFFER) item.id else null,
                    "parmonedaid" to item.pairId,
                    "cantidadejecutada" to item.executedQuantity,
                    "cantidadcancelada" to item.remainingQuantity,
                    "montoreembolsado" to item.refundAmount,
                    "fechacancelacion" to now
                )
            ).first()

            insertWalletMovement(
                usuarioId = usuarioId,
                monedaId = refundCurrencyId,
                tipoMovimiento = "Reembolso",
                monto = item.refundAmount,
                saldoAnterior = before,
                saldoPosterior = after,
                referenciaTipo = "cancelacionesordenoferta",
                referenciaId = cancelacion.cancelacionId,
                fecha = now
            )

            api.insertHistorial(
                mapOf(
                    "usuarioid" to usuarioId,
                    "tipooperacion" to "Cancelacion",
                    "referenciaid" to cancelacion.cancelacionId,
                    "parmonedaid" to item.pairId,
                    "monedaid" to refundCurrencyId,
                    "fechahora" to now,
                    "estado" to "Cancelada",
                    "metodoejecucion" to "Normal"
                )
            )

            api.insertHistorial(
                mapOf(
                    "usuarioid" to usuarioId,
                    "tipooperacion" to if (item.type == ActiveTradeType.BUY_ORDER) "Orden de compra" else "Oferta de venta",
                    "referenciaid" to item.id,
                    "parmonedaid" to item.pairId,
                    "monedaid" to refundCurrencyId,
                    "fechahora" to now,
                    "estado" to "Cancelada",
                    "metodoejecucion" to "Cancelación"
                )
            )

            invalidateMarketCache()
            AppSession.notifyWalletChanged()

            "Operación cancelada. Se reembolsaron %.2f %s."
                .format(item.refundAmount, item.refundCurrency)
        }
    }

    suspend fun loadTransactionHistory(usuarioId: Int): Result<HistoryDataUi> {
        return runCatching {
            val history = api.getHistorialByUsuario("eq.$usuarioId", limit = 200)

            val buyOrders = mutableListOf<OrderOrOffer>()
            val sellOffers = mutableListOf<OrderOrOffer>()
            val instantBuys = mutableListOf<InstantTransaction>()
            val instantSells = mutableListOf<InstantTransaction>()

            history.forEach { item ->
                when (item.tipoOperacion) {
                    "Orden de compra" -> mapHistoryOrder(item, TransactionType.BUY_ORDER)?.let { buyOrders.add(it) }
                    "Oferta de venta" -> mapHistoryOffer(item, TransactionType.SELL_OFFER)?.let { sellOffers.add(it) }
                    "Compra inmediata" -> mapHistoryInstant(item, TransactionType.INSTANT_BUY)?.let { instantBuys.add(it) }
                    "Venta inmediata" -> mapHistoryInstant(item, TransactionType.INSTANT_SELL)?.let { instantSells.add(it) }
                }
            }

            HistoryDataUi(
                buyOrders = buyOrders,
                sellOffers = sellOffers,
                instantBuys = instantBuys,
                instantSells = instantSells
            )
        }
    }

    private suspend fun mapHistoryOrder(
        history: HistorialTransaccionDto,
        type: TransactionType
    ): OrderOrOffer? {
        val order = api.getOrdenCompraById("eq.${history.referenciaId}").firstOrNull() ?: return null
        val pairText = getPairText(order.parMonedaId)
        return OrderOrOffer(
            id = "H-${history.historialId}",
            date = parseHistoryDate(history.fechaHora),
            pair = pairText,
            unitPrice = order.precioUnitario,
            originalQuantity = order.cantidadOriginal,
            originalTotal = order.totalComprometido,
            executedQuantity = order.cantidadObtenida,
            executedTotal = order.totalEjecutado,
            remainingQuantity = order.cantidadPendiente,
            remainingTotal = max(0.0, order.totalComprometido - order.totalEjecutado),
            type = type,
            state = history.estado,
            eventLabel = history.metodoEjecucion ?: "Normal"
        )
    }

    private suspend fun mapHistoryOffer(
        history: HistorialTransaccionDto,
        type: TransactionType
    ): OrderOrOffer? {
        val offer = api.getOfertaVentaById("eq.${history.referenciaId}").firstOrNull() ?: return null
        val pairText = getPairText(offer.parMonedaId)
        return OrderOrOffer(
            id = "H-${history.historialId}",
            date = parseHistoryDate(history.fechaHora),
            pair = pairText,
            unitPrice = offer.precioUnitario,
            originalQuantity = offer.cantidadOriginal,
            originalTotal = offer.totalEsperado,
            executedQuantity = offer.cantidadVendida,
            executedTotal = offer.totalRecibido,
            remainingQuantity = offer.cantidadPendiente,
            remainingTotal = offer.cantidadPendiente * offer.precioUnitario,
            type = type,
            state = history.estado,
            eventLabel = history.metodoEjecucion ?: "Normal"
        )
    }

    private suspend fun mapHistoryInstant(
        history: HistorialTransaccionDto,
        type: TransactionType
    ): InstantTransaction? {
        val operation = api.getOperacionInmediataById("eq.${history.referenciaId}").firstOrNull() ?: return null
        val pairText = getPairText(operation.parMonedaId)
        val quantity = operation.cantidadEjecutada
        val total = when (type) {
            TransactionType.INSTANT_BUY -> operation.totalPagado ?: 0.0
            TransactionType.INSTANT_SELL -> operation.totalRecibido ?: 0.0
            else -> 0.0
        }
        return InstantTransaction(
            id = "H-${history.historialId}",
            date = parseHistoryDate(history.fechaHora),
            pair = pairText,
            unitPrice = operation.precioPromedio ?: 0.0,
            quantity = quantity,
            total = total,
            type = type,
            state = history.estado,
            eventLabel = history.metodoEjecucion ?: "Normal"
        )
    }

    private suspend fun getPairText(pairId: Int): String {
        val pair = api.getParMonedaById("eq.$pairId").firstOrNull() ?: return "Par $pairId"
        val from = getMonedasCached()[pair.monedaOrigenId]?.codigoIso?.trim() ?: "?"
        val to = getMonedasCached()[pair.monedaDestinoId]?.codigoIso?.trim() ?: "?"
        return "$from → $to"
    }

    private fun parseHistoryDate(value: String?): LocalDateTime {
        if (value.isNullOrBlank()) return LocalDateTime.now()
        return runCatching {
            OffsetDateTime.parse(value).toLocalDateTime()
        }.getOrElse {
            runCatching { LocalDateTime.parse(value.take(19)) }.getOrDefault(LocalDateTime.now())
        }
    }

    private suspend fun getOrCreateWallet(usuarioId: Int): BilleteraDto {
        val existing = api.getBilleteraByUser("eq.$usuarioId").firstOrNull()
        if (existing != null) return existing

        return api.insertBilletera(
            mapOf(
                "usuarioid" to usuarioId,
                "fechacreacion" to nowIso()
            )
        ).first()
    }

    private suspend fun getCurrencyByCode(code: String): MonedaDto {
        val cleaned = cleanCode(code)
        return getMonedasCached().values.firstOrNull {
            it.codigoIso.trim().equals(cleaned, ignoreCase = true)
        } ?: error("No existe la moneda $cleaned en Supabase.")
    }

    private suspend fun getPaymentMethodByName(name: String): MetodoPagoDto {
        return api.getMetodoPagoByName("eq.$name").firstOrNull()
            ?: api.getMetodosPago().firstOrNull {
                it.nombre.equals(name, ignoreCase = true)
            }
            ?: error("No existe el método de pago $name en Supabase.")
    }

    private suspend fun getPairOrThrow(
        originCurrencyId: Int,
        destinationCurrencyId: Int
    ): ParMonedaDto {
        return getParesCached().firstOrNull {
            it.monedaOrigenId == originCurrencyId && it.monedaDestinoId == destinationCurrencyId
        } ?: error("No existe el par de monedas seleccionado en Supabase.")
    }

    private suspend fun getAvailableOffers(
        pairId: Int,
        buyerId: Int
    ): List<OfertaVentaDto> {
        return api.getOfertasVentaByPair("eq.$pairId")
            .filter { it.cantidadPendiente > 0.0 }
            .filter { isActiveStatus(it.estado) }
            .sortedWith(
                compareBy<OfertaVentaDto> { it.precioUnitario }
                    .thenBy { it.fechaCreacion ?: "" }
                    .thenBy { it.ofertaVentaId }
            )
    }

    private fun planOfferExecutions(
        offers: List<OfertaVentaDto>,
        requestedAmount: Double
    ): List<PlannedOfferExecution> {
        var remaining = requestedAmount
        val planned = mutableListOf<PlannedOfferExecution>()

        for (offer in offers) {
            if (remaining <= 0.0) break

            val taken = minOf(remaining, offer.cantidadPendiente)

            planned.add(
                PlannedOfferExecution(
                    offer = offer,
                    amountTaken = taken,
                    subtotal = taken * offer.precioUnitario
                )
            )

            remaining -= taken
        }

        return planned
    }

    private fun planSellOfferUiExecutions(
        offers: List<SellOfferUi>,
        requestedAmount: Double
    ): List<Pair<SellOfferUi, Double>> {
        var remaining = requestedAmount
        val planned = mutableListOf<Pair<SellOfferUi, Double>>()
        for (offer in offers) {
            if (remaining <= 0.0) break
            val taken = minOf(remaining, offer.quantity)
            planned.add(offer to taken)
            remaining -= taken
        }
        return planned
    }

    private fun planBuyOrderUiExecutions(
        orders: List<BuyOrderUi>,
        requestedAmount: Double
    ): List<Pair<BuyOrderUi, Double>> {
        var remaining = requestedAmount
        val planned = mutableListOf<Pair<BuyOrderUi, Double>>()
        for (order in orders) {
            if (remaining <= 0.0) break
            val taken = minOf(remaining, order.quantity)
            planned.add(order to taken)
            remaining -= taken
        }
        return planned
    }

    private suspend fun getAvailableBuyOrders(pairId: Int): List<OrdenCompraDto> {
        return api.getOrdenesCompraByPair("eq.$pairId")
            .filter { it.cantidadPendiente > 0.0 }
            .filter { isActiveStatus(it.estado) }
            .sortedWith(
                compareByDescending<OrdenCompraDto> { it.precioUnitario }
                    .thenBy { it.fechaCreacion ?: "" }
                    .thenBy { it.ordenCompraId }
            )
    }

    private fun planBuyOrderExecutions(
        orders: List<OrdenCompraDto>,
        requestedAmount: Double
    ): List<PlannedBuyOrderExecution> {
        var remaining = requestedAmount
        val planned = mutableListOf<PlannedBuyOrderExecution>()

        for (order in orders) {
            if (remaining <= 0.0) break

            val taken = minOf(remaining, order.cantidadPendiente)
            planned.add(
                PlannedBuyOrderExecution(
                    order = order,
                    amountTaken = taken,
                    subtotal = taken * order.precioUnitario
                )
            )

            remaining -= taken
        }

        return planned
    }

    private suspend fun getInversePair(pair: ParMonedaDto): ParMonedaDto? {
        return getParesCached().firstOrNull {
            it.monedaOrigenId == pair.monedaDestinoId && it.monedaDestinoId == pair.monedaOrigenId
        }
    }

    private suspend fun getAvailableBalance(
        usuarioId: Int,
        monedaId: Int
    ): Double {
        val wallet = getOrCreateWallet(usuarioId)
        val saldo = getOrCreateSaldo(wallet.billeteraId, monedaId)
        return saldo.saldoDisponible
    }

    private suspend fun getOrCreateSaldo(
        billeteraId: Int,
        monedaId: Int
    ): SaldoBilleteraDto {
        val existing = api.getSaldo(
            billeteraId = "eq.$billeteraId",
            monedaId = "eq.$monedaId"
        ).firstOrNull()

        if (existing != null) return existing

        return api.insertSaldo(
            mapOf(
                "billeteraid" to billeteraId,
                "monedaid" to monedaId,
                "saldodisponible" to 0.0,
                "fechaactualizacion" to nowIso()
            )
        ).first()
    }

    private suspend fun addBalance(
        usuarioId: Int,
        monedaId: Int,
        delta: Double
    ): Pair<Double, Double> {
        val wallet = getOrCreateWallet(usuarioId)
        val saldo = getOrCreateSaldo(wallet.billeteraId, monedaId)

        val before = saldo.saldoDisponible
        val after = before + delta

        api.updateSaldo(
            saldoId = "eq.${saldo.saldoId}",
            body = mapOf(
                "saldodisponible" to after,
                "fechaactualizacion" to nowIso()
            )
        )

        return before to after
    }

    private suspend fun subtractBalance(
        usuarioId: Int,
        monedaId: Int,
        amount: Double
    ): Pair<Double, Double> {
        val wallet = getOrCreateWallet(usuarioId)
        val saldo = getOrCreateSaldo(wallet.billeteraId, monedaId)

        val before = saldo.saldoDisponible
        require(before >= amount) { "Saldo insuficiente" }

        val after = before - amount

        api.updateSaldo(
            saldoId = "eq.${saldo.saldoId}",
            body = mapOf(
                "saldodisponible" to after,
                "fechaactualizacion" to nowIso()
            )
        )

        return before to after
    }

    private suspend fun insertWalletMovement(
        usuarioId: Int,
        monedaId: Int,
        tipoMovimiento: String,
        monto: Double,
        saldoAnterior: Double,
        saldoPosterior: Double,
        referenciaTipo: String,
        referenciaId: Int,
        fecha: String
    ) {
        api.insertMovimiento(
            mapOf(
                "usuarioid" to usuarioId,
                "monedaid" to monedaId,
                "tipomovimiento" to tipoMovimiento,
                "monto" to monto,
                "saldoanterior" to saldoAnterior,
                "saldoposterior" to saldoPosterior,
                "fechamovimiento" to fecha,
                "referenciatipo" to referenciaTipo,
                "referenciaid" to referenciaId
            )
        )
    }

    private fun formatDate(value: String?): String {
        if (value.isNullOrBlank()) return "-"
        return value.take(16).replace("T", " ")
    }

    suspend fun loadHomeData(usuarioId: Int): HomeData = coroutineScope {
        val currentUser = runCatching { api.getUsuarioById("eq.$usuarioId").firstOrNull() }.getOrNull()
        val isRestricted = currentUser?.estado?.equals("Restringido", ignoreCase = true) == true
        val restrictionReason = if (isRestricted) getActiveRestrictionReason(usuarioId).getOrNull() else null

        val allPairs = getAllPairs().getOrDefault(emptyList())
            .filter { it.volume > 0.0 }
            .sortedByDescending { it.volume }

        var globalChart: com.example.exchangededivisas.data.model.CurrencyPairChartData? = null
        for (pair in allPairs) {
            val chart = getPairChartData("${pair.fromCode}_${pair.toCode}").getOrNull()
            if (chart != null && chart.prices.isNotEmpty()) {
                globalChart = chart
                break
            }
        }

        val recentPairIds = getRecentPairIds(usuarioId)
        val paresById = getParesCached().associateBy { it.parMonedaId }
        val monedas = getMonedasCached()
        var userChart: com.example.exchangededivisas.data.model.CurrencyPairChartData? = null

        for (pairId in recentPairIds) {
            val pair = paresById[pairId] ?: continue
            val fromCode = monedas[pair.monedaOrigenId]?.codigoIso?.trim() ?: continue
            val toCode = monedas[pair.monedaDestinoId]?.codigoIso?.trim() ?: continue
            val chart = getPairChartData("${fromCode}_${toCode}").getOrNull()
            if (chart != null && chart.prices.isNotEmpty()) {
                userChart = chart
                break
            }
        }

        HomeData(
            globalMostActive = globalChart,
            userMostActive = userChart ?: allPairs.drop(1).firstNotNullOfOrNull { pair ->
                getPairChartData("${pair.fromCode}_${pair.toCode}").getOrNull()?.takeIf { it.prices.isNotEmpty() }
            } ?: globalChart,
            hasUserActivity = recentPairIds.isNotEmpty(),
            isRestricted = isRestricted,
            restrictionReason = restrictionReason
        )
    }

    private suspend fun buildChartDataOptimized(
        parId: Int,
        monedasMap: Map<Int, MonedaDto>
    ): com.example.exchangededivisas.data.model.CurrencyPairChartData {
        val cached = chartCache[parId]
        if (cached != null && cached.isFresh(MARKET_CACHE_MS)) return cached.value

        val par = api.getParMonedaById("eq.$parId").firstOrNull()
            ?: return com.example.exchangededivisas.data.model.CurrencyPairChartData("?", "?", emptyList())

        val fromCode = monedasMap[par.monedaOrigenId]?.codigoIso?.trim() ?: "?"
        val toCode = monedasMap[par.monedaDestinoId]?.codigoIso?.trim() ?: "?"

        val historico = try {
            api.getHistoricoByPar(
                parMonedaId = "eq.$parId",
                order = "fecharegistro.asc",
                limit = 1000
            )
        } catch (e: Exception) { emptyList() }

        val prices = historico.mapNotNull { h ->
            val buy = h.mayorPrecioCompra ?: return@mapNotNull null
            val sell = h.menorPrecioVenta ?: return@mapNotNull null
            val fecha = h.fechaRegistro ?: return@mapNotNull null
            try {
                val dt = OffsetDateTime.parse(fecha).toLocalDateTime()
                com.example.exchangededivisas.data.model.HistoricalPrice(dt, buy, sell)
            } catch (e: Exception) { null }
        }

        val data = com.example.exchangededivisas.data.model.CurrencyPairChartData(fromCode, toCode, prices)
        chartCache[parId] = TimedCache(data)
        return data
    }

    data class HomeData(
        val globalMostActive: com.example.exchangededivisas.data.model.CurrencyPairChartData?,
        val userMostActive: com.example.exchangededivisas.data.model.CurrencyPairChartData?,
        val hasUserActivity: Boolean,
        val isRestricted: Boolean = false,
        val restrictionReason: String? = null
    )
    // ── PARES DE MONEDAS ─────────────────────────────────────────────────────

    data class PairUi(
        val parMonedaId: Int,
        val fromCode: String,
        val toCode: String,
        val fromName: String,
        val toName: String,
        val bestBuy: Double?,
        val bestSell: Double?,
        val margin: Double?,
        val volume: Double
    )

    data class SellOfferUi(
        val ofertaId: Int,
        val quantity: Double,
        val price: Double,
        val total: Double
    )

    data class BuyOrderUi(
        val ordenId: Int,
        val quantity: Double,
        val price: Double,
        val total: Double
    )

    data class InstantSalePreview(
        val pairCode: String,
        val baseCurrency: String,
        val quoteCurrency: String,
        val requestedAmount: Double,
        val coveredAmount: Double,
        val totalToReceive: Double,
        val minPrice: Double,
        val maxPrice: Double,
        val avgPrice: Double,
        val availableBalance: Double,
        val hasLiquidity: Boolean,
        val hasEnoughBalance: Boolean
    )

    data class InstantSaleReceipt(
        val operationId: Int,
        val soldAmount: Double,
        val receivedTotal: Double,
        val baseCurrency: String,
        val quoteCurrency: String
    )

    data class WithdrawItem(
        val monedaId: Int,
        val code: String,
        val amount: Double
    )

    data class WithdrawReceipt(
        val totalWithdrawn: Map<String, Double>,
        val commission: Double,
        val netReceived: Double
    )

    /** Carga todos los pares con puntas directas + espejo inverso.
     * Volumen se expresa siempre en la moneda destino del par visible.
     */
    suspend fun getAllPairs(forceRefresh: Boolean = false): Result<List<PairUi>> {
        return runCatching {
            val cached = cachedAllPairsUi
            if (!forceRefresh && cached != null && cached.isFresh(MARKET_CACHE_MS)) return@runCatching cached.value

            val monedas = getMonedasCached(forceRefresh)
            val pares = getParesCached(forceRefresh)
            val pairBySides = pares.associateBy { it.monedaOrigenId to it.monedaDestinoId }
            val offersByPair = getAllOffersCached(forceRefresh).groupBy { it.parMonedaId }
            val ordersByPair = getAllOrdersCached(forceRefresh).groupBy { it.parMonedaId }

            val result = pares.mapNotNull { par ->
                val from = monedas[par.monedaOrigenId] ?: return@mapNotNull null
                val to = monedas[par.monedaDestinoId] ?: return@mapNotNull null
                val inverse = pairBySides[par.monedaDestinoId to par.monedaOrigenId]

                val directOffers = offersByPair[par.parMonedaId].orEmpty()
                val directOrders = ordersByPair[par.parMonedaId].orEmpty()
                val inverseOffers = inverse?.let { offersByPair[it.parMonedaId].orEmpty() }.orEmpty()
                val inverseOrders = inverse?.let { ordersByPair[it.parMonedaId].orEmpty() }.orEmpty()

                val visibleSellPrices = directOffers.map { it.precioUnitario } +
                        inverseOrders.mapNotNull { if (it.precioUnitario > 0.0) 1.0 / it.precioUnitario else null }
                val visibleBuyPrices = directOrders.map { it.precioUnitario } +
                        inverseOffers.mapNotNull { if (it.precioUnitario > 0.0) 1.0 / it.precioUnitario else null }

                val bestSell = visibleSellPrices.minOrNull()
                val bestBuy = visibleBuyPrices.maxOrNull()
                val margin = if (bestBuy != null && bestSell != null) bestSell - bestBuy else null

                val directVolumeDestino = directOffers.sumOf { it.cantidadPendiente } + directOrders.sumOf { it.cantidadPendiente }
                val mirroredVolumeDestino = inverseOffers.sumOf { it.cantidadPendiente * it.precioUnitario } +
                        inverseOrders.sumOf { it.cantidadPendiente * it.precioUnitario }
                val volume = directVolumeDestino + mirroredVolumeDestino

                PairUi(
                    parMonedaId = par.parMonedaId,
                    fromCode = from.codigoIso.trim(),
                    toCode = to.codigoIso.trim(),
                    fromName = from.nombre,
                    toName = to.nombre,
                    bestBuy = bestBuy,
                    bestSell = bestSell,
                    margin = margin,
                    volume = volume
                )
            }

            cachedAllPairsUi = TimedCache(result)
            result
        }
    }

    /** Devuelve los parMonedaId con los que el usuario operó recientemente (más reciente primero). */
    suspend fun getRecentPairIds(usuarioId: Int): List<Int> {
        return runCatching {
            api.getHistorialByUsuario("eq.$usuarioId")
                .mapNotNull { it.parMonedaId }
                .distinct()
        }.getOrElse { emptyList() }
    }

    /** Historial de precios real de un par para el gráfico.
     *
     * Si el par directo no tiene snapshots, intenta usar el par inverso e invierte
     * los precios. Así USD/PEN puede graficarse aunque los snapshots originales
     * hayan sido generados desde PEN/USD.
     */
    suspend fun getPairChartData(
        pairCode: String
    ): Result<com.example.exchangededivisas.data.model.CurrencyPairChartData> {
        return runCatching {
            val (fromCode, toCode) = parsePairCode(pairCode)
            val from = getCurrencyByCode(fromCode)
            val to   = getCurrencyByCode(toCode)
            val pair = getPairOrThrow(from.monedaId, to.monedaId)

            val allMonedas = getMonedasCached()
            val direct = buildChartDataOptimized(pair.parMonedaId, allMonedas)
            if (direct.prices.isNotEmpty()) return@runCatching direct

            val inverse = getInversePair(pair)
            val result = if (inverse != null) {
                val inverseData = buildChartDataOptimized(inverse.parMonedaId, allMonedas)
                val inverted = inverseData.prices.mapNotNull { point ->
                    if (point.buyPrice <= 0.0 || point.sellPrice <= 0.0) null
                    else com.example.exchangededivisas.data.model.HistoricalPrice(
                        timestamp = point.timestamp,
                        buyPrice = 1.0 / point.sellPrice,
                        sellPrice = 1.0 / point.buyPrice
                    )
                }
                com.example.exchangededivisas.data.model.CurrencyPairChartData(
                    baseCurrency = from.codigoIso.trim(),
                    quoteCurrency = to.codigoIso.trim(),
                    prices = inverted
                )
            } else {
                direct
            }

            if (result.prices.isNotEmpty()) return@runCatching result

            val current = getAllPairs(forceRefresh = true).getOrDefault(emptyList())
                .firstOrNull { it.fromCode == from.codigoIso.trim() && it.toCode == to.codigoIso.trim() }

            if (current?.bestBuy != null && current.bestSell != null) {
                com.example.exchangededivisas.data.model.CurrencyPairChartData(
                    baseCurrency = from.codigoIso.trim(),
                    quoteCurrency = to.codigoIso.trim(),
                    prices = listOf(
                        com.example.exchangededivisas.data.model.HistoricalPrice(
                            timestamp = LocalDateTime.now(),
                            buyPrice = current.bestBuy,
                            sellPrice = current.bestSell
                        )
                    )
                )
            } else {
                result
            }
        }
    }

    /** Ofertas de venta activas de un par (libro de órdenes).
     *
     * Incluye el espejo de las órdenes de compra del par inverso.
     * Ejemplo: una orden PEN -> USD se ve como oferta USD -> PEN con precio 1/precio.
     */
    suspend fun getActiveSellOffers(pairCode: String): Result<List<SellOfferUi>> {
        return runCatching {
            val (fromCode, toCode) = parsePairCode(pairCode)
            val from = getCurrencyByCode(fromCode)
            val to   = getCurrencyByCode(toCode)
            val pair = getPairOrThrow(from.monedaId, to.monedaId)
            val inversePair = getInversePair(pair)

            val directOffers = api.getOfertasVentaByPair("eq.${pair.parMonedaId}")
                .filter { isActiveStatus(it.estado) && it.cantidadPendiente > 0.0 }
                .map {
                    SellOfferUi(
                        ofertaId = it.ofertaVentaId,
                        quantity = it.cantidadPendiente,
                        price    = it.precioUnitario,
                        total    = it.cantidadPendiente * it.precioUnitario
                    )
                }

            val mirroredOrders = inversePair?.let { inv ->
                api.getOrdenesCompraByPair("eq.${inv.parMonedaId}")
                    .filter { isActiveStatus(it.estado) && it.cantidadPendiente > 0.0 && it.precioUnitario > 0.0 }
                    .map {
                        val invertedPrice = 1.0 / it.precioUnitario
                        val visibleQuantity = it.cantidadPendiente * it.precioUnitario
                        SellOfferUi(
                            ofertaId = -it.ordenCompraId,
                            quantity = visibleQuantity,
                            price    = invertedPrice,
                            total    = visibleQuantity * invertedPrice
                        )
                    }
            }.orEmpty()

            (directOffers + mirroredOrders)
                .sortedWith(compareBy<SellOfferUi> { it.price }.thenBy { it.ofertaId })
        }
    }

    /** Órdenes de compra activas de un par (libro de órdenes).
     *
     * Incluye el espejo de las ofertas de venta del par inverso.
     * Ejemplo: una oferta PEN -> USD se ve como orden USD -> PEN con precio 1/precio.
     */
    suspend fun getActiveBuyOrders(pairCode: String): Result<List<BuyOrderUi>> {
        return runCatching {
            val (fromCode, toCode) = parsePairCode(pairCode)
            val from = getCurrencyByCode(fromCode)
            val to   = getCurrencyByCode(toCode)
            val pair = getPairOrThrow(from.monedaId, to.monedaId)
            val inversePair = getInversePair(pair)

            val directOrders = api.getOrdenesCompraByPair("eq.${pair.parMonedaId}")
                .filter { isActiveStatus(it.estado) && it.cantidadPendiente > 0.0 }
                .map {
                    BuyOrderUi(
                        ordenId  = it.ordenCompraId,
                        quantity = it.cantidadPendiente,
                        price    = it.precioUnitario,
                        total    = it.cantidadPendiente * it.precioUnitario
                    )
                }

            val mirroredOffers = inversePair?.let { inv ->
                api.getOfertasVentaByPair("eq.${inv.parMonedaId}")
                    .filter { isActiveStatus(it.estado) && it.cantidadPendiente > 0.0 && it.precioUnitario > 0.0 }
                    .map {
                        val invertedPrice = 1.0 / it.precioUnitario
                        val visibleQuantity = it.cantidadPendiente * it.precioUnitario
                        BuyOrderUi(
                            ordenId  = -it.ofertaVentaId,
                            quantity = visibleQuantity,
                            price    = invertedPrice,
                            total    = visibleQuantity * invertedPrice
                        )
                    }
            }.orEmpty()

            (directOrders + mirroredOffers)
                .sortedWith(compareByDescending<BuyOrderUi> { it.price }.thenBy { it.ordenId })
        }
    }

    /** Inserta una oferta de venta del usuario */
    suspend fun createSellOffer(
        usuarioId: Int,
        pairCode: String,
        amount: Double,
        price: Double
    ): Result<Unit> {
        return runCatching {
            require(amount > 0.0) { "Cantidad inválida" }
            require(price > 0.0)  { "Precio inválido" }
            ensureUserCanOperate(usuarioId, "generar ofertas de venta")

            val (fromCode, toCode) = parsePairCode(pairCode)
            val from = getCurrencyByCode(fromCode)
            val to   = getCurrencyByCode(toCode)
            val pair = getPairOrThrow(from.monedaId, to.monedaId)

            val available = getAvailableBalance(usuarioId, to.monedaId)
            require(available >= amount) { "Saldo insuficiente" }

            val now = nowIso()
            val (before, after) = subtractBalance(usuarioId, to.monedaId, amount)

            val oferta = api.insertOfertaVenta(
                mapOf(
                    "usuarioid"          to usuarioId,
                    "parmonedaid"        to pair.parMonedaId,
                    "cantidadoriginal"   to amount,
                    "cantidadvendida"    to 0.0,
                    "cantidadpendiente"  to amount,
                    "preciounitario"     to price,
                    "totalesperado"      to amount * price,
                    "totalrecibido"      to 0.0,
                    "estado"             to "Activa",
                    "fechacreacion"      to now,
                    "fechaactualizacion" to now,
                    "fechacancelacion"   to null,
                    "ordencompraespejoid" to null
                )
            ).first()

            insertWalletMovement(
                usuarioId, to.monedaId, "OfertaVenta", amount,
                before, after, "ofertasventa", oferta.ofertaVentaId, now
            )

            api.insertHistorial(mapOf(
                "usuarioid"       to usuarioId,
                "tipooperacion"   to "Oferta de venta",
                "referenciaid"    to oferta.ofertaVentaId,
                "parmonedaid"     to pair.parMonedaId,
                "monedaid"        to null,
                "fechahora"       to now,
                "estado"          to "Activa",
                "metodoejecucion" to "Normal"
            ))

            val correo = api.getUsuarioById("eq.$usuarioId").firstOrNull()
                ?.correoElectronico ?: "correo@pendiente.com"
            val asuntoOferta = "Tu oferta de venta fue registrada"
            val cuerpoOferta = "Oferta de %.4f %s a precio %.4f %s creada correctamente."
                .format(amount, to.codigoIso.trim(), price, from.codigoIso.trim())
            val ofertaNotificacion = api.insertNotificacionCorreo(mapOf(
                "usuarioid"         to usuarioId,
                "tiponotificacionid" to null,
                "correodestino"     to correo,
                "tipoevento"        to "OFERTA_CREADA",
                "asunto"            to asuntoOferta,
                "cuerpo"            to cuerpoOferta,
                "estadoenvio"       to "Pendiente",
                "fechacreacion"     to now,
                "fechaenvio"        to null,
                "referenciatipo"    to "ofertasventa",
                "referenciaid"      to oferta.ofertaVentaId
            )).firstOrNull()

            notifyNative(usuarioId, asuntoOferta, cuerpoOferta, ofertaNotificacion?.notificacionId)
            dispatchPendingEmails()

            invalidateMarketCache()
            AppSession.notifyWalletChanged()
        }
    }

    /** Genera una ORDEN DE COMPRA del usuario */
    suspend fun createBuyOrder(
        usuarioId: Int,
        pairCode: String,
        amount: Double,
        price: Double
    ): Result<Unit> {
        return runCatching {
            require(amount > 0.0) { "Cantidad inválida" }
            require(price > 0.0)  { "Precio inválido" }
            ensureUserCanOperate(usuarioId, "generar órdenes de compra")

            val (fromCode, toCode) = parsePairCode(pairCode)
            val from = getCurrencyByCode(fromCode)
            val to   = getCurrencyByCode(toCode)
            val pair = getPairOrThrow(from.monedaId, to.monedaId)

            val total = amount * price

            val available = getAvailableBalance(usuarioId, from.monedaId)
            require(available >= total) { "Saldo insuficiente" }

            val now = nowIso()
            val (before, after) = subtractBalance(usuarioId, from.monedaId, total)

            val orden = api.insertOrdenCompra(
                mapOf(
                    "usuarioid"          to usuarioId,
                    "parmonedaid"        to pair.parMonedaId,
                    "cantidadoriginal"   to amount,
                    "cantidadobtenida"   to 0.0,
                    "cantidadpendiente"  to amount,
                    "preciounitario"     to price,
                    "totalcomprometido"  to total,
                    "totalejecutado"     to 0.0,
                    "estado"             to "Activa",
                    "fechacreacion"      to now,
                    "fechaactualizacion" to now,
                    "fechacancelacion"   to null
                )
            ).first()

            insertWalletMovement(
                usuarioId, from.monedaId, "OrdenCompra", total,
                before, after, "ordenescompra", orden.ordenCompraId, now
            )

            api.insertHistorial(mapOf(
                "usuarioid"       to usuarioId,
                "tipooperacion"   to "Orden de compra",
                "referenciaid"    to orden.ordenCompraId,
                "parmonedaid"     to pair.parMonedaId,
                "monedaid"        to null,
                "fechahora"       to now,
                "estado"          to "Activa",
                "metodoejecucion" to "Normal"
            ))

            val correo = api.getUsuarioById("eq.$usuarioId").firstOrNull()
                ?.correoElectronico ?: "correo@pendiente.com"
            val asuntoOrden = "Tu orden de compra fue registrada"
            val cuerpoOrden = "Orden de %.4f %s a precio %.4f %s creada correctamente."
                .format(amount, to.codigoIso.trim(), price, from.codigoIso.trim())
            val ordenNotificacion = api.insertNotificacionCorreo(mapOf(
                "usuarioid"          to usuarioId,
                "tiponotificacionid" to null,
                "correodestino"      to correo,
                "tipoevento"         to "ORDEN_CREADA",
                "asunto"             to asuntoOrden,
                "cuerpo"             to cuerpoOrden,
                "estadoenvio"        to "Pendiente",
                "fechacreacion"      to now,
                "fechaenvio"         to null,
                "referenciatipo"     to "ordenescompra",
                "referenciaid"       to orden.ordenCompraId
            )).firstOrNull()

            notifyNative(usuarioId, asuntoOrden, cuerpoOrden, ordenNotificacion?.notificacionId)
            dispatchPendingEmails()

            invalidateMarketCache()
            AppSession.notifyWalletChanged()
        }
    }

    /** Preview de venta inmediata (sin ejecutar) */
    suspend fun previewInstantSale(
        usuarioId: Int,
        pairCode: String,
        amount: Double
    ): Result<InstantSalePreview> {
        return runCatching {
            require(amount > 0.0) { "Valor inválido" }
            ensureUserCanOperate(usuarioId, "vender inmediatamente")

            val (baseCode, quoteCode) = parsePairCode(pairCode)
            val base  = getCurrencyByCode(baseCode)
            val quote = getCurrencyByCode(quoteCode)

            val orders = getActiveBuyOrders(pairCode).getOrThrow()
            val planned = planBuyOrderUiExecutions(orders, amount)

            val covered = planned.sumOf { it.second }
            val totalReceive = planned.sumOf { (order, taken) -> taken * order.price }
            val prices = planned.map { it.first.price }
            val available = getAvailableBalance(usuarioId, quote.monedaId)

            InstantSalePreview(
                pairCode = "${base.codigoIso.trim()}_${quote.codigoIso.trim()}",
                baseCurrency = base.codigoIso.trim(),
                quoteCurrency = quote.codigoIso.trim(),
                requestedAmount = amount,
                coveredAmount = covered,
                totalToReceive = totalReceive,
                minPrice = prices.minOrNull() ?: 0.0,
                maxPrice = prices.maxOrNull() ?: 0.0,
                avgPrice = if (covered > 0.0) totalReceive / covered else 0.0,
                availableBalance = available,
                hasLiquidity = covered >= amount,
                hasEnoughBalance = available >= amount
            )
        }
    }

    /** Ejecuta la venta inmediata de forma transaccional en Supabase. */
    suspend fun executeInstantSale(
        usuarioId: Int,
        pairCode: String,
        amount: Double
    ): Result<InstantSaleReceipt> {
        return runCatching {
            require(amount > 0.0) { "Valor inválido" }
            ensureUserCanOperate(usuarioId, "vender inmediatamente")

            val (baseCode, quoteCode) = parsePairCode(pairCode)
            val base  = getCurrencyByCode(baseCode)
            val quote = getCurrencyByCode(quoteCode)
            val pair  = getPairOrThrow(base.monedaId, quote.monedaId)

            val result = api.ejecutarVentaInmediataSegura(
                mapOf(
                    "p_usuarioid" to usuarioId,
                    "p_parmonedaid" to pair.parMonedaId,
                    "p_cantidad" to amount
                )
            )

            val operationId = result.operacionId
                ?: error(result.mensaje ?: "No se pudo ejecutar la venta inmediata.")

            invalidateMarketCache()
            AppSession.notifyWalletChanged()

            notifyNative(
                usuarioId,
                "Venta inmediata completada",
                "Vendiste %.4f %s y recibiste %.4f %s.".format(
                    result.cantidadVendida ?: amount,
                    result.monedaVendida ?: base.codigoIso.trim(),
                    result.totalRecibido ?: 0.0,
                    result.monedaRecibida ?: quote.codigoIso.trim()
                )
            )
            dispatchPendingEmails()

            InstantSaleReceipt(
                operationId = operationId,
                soldAmount = result.cantidadVendida ?: amount,
                receivedTotal = result.totalRecibido ?: 0.0,
                baseCurrency = result.monedaRecibida ?: base.codigoIso.trim(),
                quoteCurrency = result.monedaVendida ?: quote.codigoIso.trim()
            )
        }
    }

    /** Ejecuta retiro descontando saldo real de la billetera. La comisión se calcula por moneda. */
    suspend fun executeWithdraw(
        usuarioId: Int,
        items: List<WithdrawItem>,
        metodoPagoId: Int,
        comisionPorcentaje: Double,
        comisionFija: Double = 0.0
    ): Result<WithdrawReceipt> {
        return runCatching {
            require(items.isNotEmpty()) { "Selecciona al menos una moneda" }
            items.forEach {
                require(it.amount > 0.0) { "Monto inválido" }
            }

            val now = nowIso()

            var totalCommission = 0.0
            var totalNetReceived = 0.0

            items.forEach { item ->
                val (before, after) = subtractBalance(
                    usuarioId = usuarioId,
                    monedaId = item.monedaId,
                    amount = item.amount
                )

                val comisionItem = item.amount * (comisionPorcentaje / 100.0) + comisionFija
                val netItem = max(0.0, item.amount - comisionItem)

                totalCommission += comisionItem
                totalNetReceived += netItem

                val retiro = api.insertRetiro(
                    mapOf(
                        "usuarioid" to usuarioId,
                        "monedaid" to item.monedaId,
                        "metodopagoid" to metodoPagoId,
                        "montoretirado" to item.amount,
                        "comisionaplicada" to comisionItem,
                        "montofinalrecibido" to netItem,
                        "estado" to "Completada",
                        "voucherurl" to null,
                        "fecharetiro" to now
                    )
                ).first()

                insertWalletMovement(
                    usuarioId = usuarioId,
                    monedaId = item.monedaId,
                    tipoMovimiento = "Retiro",
                    monto = item.amount,
                    saldoAnterior = before,
                    saldoPosterior = after,
                    referenciaTipo = "retiros",
                    referenciaId = retiro.retiroId,
                    fecha = now
                )

                api.insertHistorial(
                    mapOf(
                        "usuarioid" to usuarioId,
                        "tipooperacion" to "Retiro",
                        "referenciaid" to retiro.retiroId,
                        "parmonedaid" to null,
                        "monedaid" to item.monedaId,
                        "fechahora" to now,
                        "estado" to "Completada",
                        "metodoejecucion" to "Normal"
                    )
                )

                val user = api.getUsuarioById("eq.$usuarioId").firstOrNull()
                val correo = user?.correoElectronico ?: "correo@pendiente.com"

                val asuntoRetiro = "Voucher de retiro Ezchange"
                val cuerpoRetiro = "Se registró un retiro de %.2f %s. Comisión: %.2f. Monto final recibido: %.2f."
                    .format(item.amount, item.code, comisionItem, netItem)

                val retiroNotificacion = api.insertNotificacionCorreo(
                    mapOf(
                        "usuarioid" to usuarioId,
                        "tiponotificacionid" to null,
                        "correodestino" to correo,
                        "tipoevento" to "VOUCHER_RETIRO",
                        "asunto" to asuntoRetiro,
                        "cuerpo" to cuerpoRetiro,
                        "estadoenvio" to "Pendiente",
                        "fechacreacion" to now,
                        "fechaenvio" to null,
                        "referenciatipo" to "retiros",
                        "referenciaid" to retiro.retiroId
                    )
                ).firstOrNull()

                notifyNative(usuarioId, asuntoRetiro, cuerpoRetiro, retiroNotificacion?.notificacionId)
                dispatchPendingEmails()
            }

            AppSession.notifyWalletChanged()

            WithdrawReceipt(
                totalWithdrawn = items.associate { it.code to it.amount },
                commission = totalCommission,
                netReceived = totalNetReceived
            )
        }
    }
}
