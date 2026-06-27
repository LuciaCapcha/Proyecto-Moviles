package com.example.exchangededivisas.data.repository

import com.example.exchangededivisas.api.ApiClient
import com.example.exchangededivisas.data.remote.BilleteraDto
import com.example.exchangededivisas.data.remote.MetodoPagoDto
import com.example.exchangededivisas.data.remote.MonedaDto
import com.example.exchangededivisas.data.remote.OfertaVentaDto
import com.example.exchangededivisas.data.remote.ParMonedaDto
import com.example.exchangededivisas.data.remote.SaldoBilleteraDto
import com.example.exchangededivisas.data.session.AppSession
import java.time.OffsetDateTime
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

private data class PlannedOfferExecution(
    val offer: OfertaVentaDto,
    val amountTaken: Double,
    val subtotal: Double
)

object ExchangeRepository {

    private val api = ApiClient.supabase

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
        val wallet = getOrCreateWallet(usuarioId)

        val currencies = api.getMonedas()
        val balances = api.getSaldosByWallet("eq.${wallet.billeteraId}")
            .associateBy { it.monedaId }

        return currencies.map { currency ->
            val balance = balances[currency.monedaId]?.saldoDisponible ?: 0.0

            WalletCurrencyUi(
                monedaId = currency.monedaId,
                code = currency.codigoIso.trim(),
                name = currency.nombre,
                balance = balance,
                isInternational = currency.tipo?.contains("Internacional", ignoreCase = true) == true
            )
        }.sortedByDescending { it.balance }
    }

    suspend fun makeDeposit(
        usuarioId: Int,
        currencyCode: String,
        paymentMethodName: String,
        amount: Double
    ): Result<String> {
        return runCatching {
            require(amount > 0.0) { "Monto inválido" }

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

            api.insertNotificacionCorreo(
                mapOf(
                    "usuarioid" to usuarioId,
                    "tiponotificacionid" to null,
                    "correodestino" to correo,
                    "tipoevento" to "VOUCHER_DEPOSITO",
                    "asunto" to "Voucher de depósito Ezchange",
                    "cuerpo" to "Se registró un depósito de %.2f %s. Total pagado: %.2f. Comisión: %.2f."
                        .format(amount, moneda.codigoIso.trim(), totalPaid, commission),
                    "estadoenvio" to "Pendiente",
                    "fechacreacion" to now,
                    "fechaenvio" to null,
                    "referenciatipo" to "depositos",
                    "referenciaid" to deposito.depositoId
                )
            )

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

            val offers = getAvailableOffers(pair.parMonedaId, usuarioId)
            val planned = planOfferExecutions(offers, amount)

            val covered = planned.sumOf { it.amountTaken }
            val total = planned.sumOf { it.subtotal }
            val prices = planned.map { it.offer.precioUnitario }

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

            val (fromCode, toCode) = parsePairCode(pairCode)
            val fromCurrency = getCurrencyByCode(fromCode)
            val toCurrency = getCurrencyByCode(toCode)
            val pair = getPairOrThrow(fromCurrency.monedaId, toCurrency.monedaId)

            val offers = getAvailableOffers(pair.parMonedaId, usuarioId)
            val planned = planOfferExecutions(offers, amount)

            val covered = planned.sumOf { it.amountTaken }
            require(covered >= amount) { "Liquidez insuficiente" }

            val total = planned.sumOf { it.subtotal }
            val availableBalance = getAvailableBalance(usuarioId, fromCurrency.monedaId)
            require(availableBalance >= total) { "Saldo insuficiente" }

            val prices = planned.map { it.offer.precioUnitario }
            val now = nowIso()

            val operation = api.insertOperacionInmediata(
                mapOf(
                    "usuarioid" to usuarioId,
                    "parmonedaid" to pair.parMonedaId,
                    "tipooperacion" to "Compra inmediata",
                    "metodoejecucion" to "Normal",
                    "cantidadsolicitada" to amount,
                    "cantidadejecutada" to covered,
                    "preciominimo" to (prices.minOrNull() ?: 0.0),
                    "preciomaximo" to (prices.maxOrNull() ?: 0.0),
                    "preciopromedio" to if (covered > 0.0) total / covered else 0.0,
                    "totalpagado" to total,
                    "totalrecibido" to covered,
                    "estado" to "Completada",
                    "fechaoperacion" to now,
                    "operacionpadreid" to null
                )
            ).first()

            val (buyerFromBefore, buyerFromAfter) = subtractBalance(
                usuarioId = usuarioId,
                monedaId = fromCurrency.monedaId,
                amount = total
            )

            insertWalletMovement(
                usuarioId = usuarioId,
                monedaId = fromCurrency.monedaId,
                tipoMovimiento = "CompraInmediata",
                monto = total,
                saldoAnterior = buyerFromBefore,
                saldoPosterior = buyerFromAfter,
                referenciaTipo = "operacionesinmediatas",
                referenciaId = operation.operacionInmediataId,
                fecha = now
            )

            val (buyerToBefore, buyerToAfter) = addBalance(
                usuarioId = usuarioId,
                monedaId = toCurrency.monedaId,
                delta = covered
            )

            insertWalletMovement(
                usuarioId = usuarioId,
                monedaId = toCurrency.monedaId,
                tipoMovimiento = "CompraInmediata",
                monto = covered,
                saldoAnterior = buyerToBefore,
                saldoPosterior = buyerToAfter,
                referenciaTipo = "operacionesinmediatas",
                referenciaId = operation.operacionInmediataId,
                fecha = now
            )

            planned.forEach { execution ->
                val offer = execution.offer

                val newSold = offer.cantidadVendida + execution.amountTaken
                val newPending = max(0.0, offer.cantidadPendiente - execution.amountTaken)
                val newReceived = offer.totalRecibido + execution.subtotal

                val newStatus = if (newPending <= 0.000001) {
                    "Completada"
                } else {
                    "Parcialmente ejecutada"
                }

                api.updateOfertaVenta(
                    ofertaVentaId = "eq.${offer.ofertaVentaId}",
                    body = mapOf(
                        "cantidadvendida" to newSold,
                        "cantidadpendiente" to newPending,
                        "totalrecibido" to newReceived,
                        "estado" to newStatus,
                        "fechaactualizacion" to now
                    )
                )

                val (sellerBefore, sellerAfter) = addBalance(
                    usuarioId = offer.usuarioId,
                    monedaId = fromCurrency.monedaId,
                    delta = execution.subtotal
                )

                insertWalletMovement(
                    usuarioId = offer.usuarioId,
                    monedaId = fromCurrency.monedaId,
                    tipoMovimiento = "VentaInmediata",
                    monto = execution.subtotal,
                    saldoAnterior = sellerBefore,
                    saldoPosterior = sellerAfter,
                    referenciaTipo = "operacionesinmediatas",
                    referenciaId = operation.operacionInmediataId,
                    fecha = now
                )

                val seller = api.getUsuarioById("eq.${offer.usuarioId}").firstOrNull()
                val sellerEmail = seller?.correoElectronico ?: "correo@pendiente.com"

                api.insertNotificacionCorreo(
                    mapOf(
                        "usuarioid" to offer.usuarioId,
                        "tiponotificacionid" to null,
                        "correodestino" to sellerEmail,
                        "tipoevento" to "PROGRESO_OFERTA",
                        "asunto" to "Tu oferta recibió una compra",
                        "cuerpo" to "Se ejecutaron %.2f %s de tu oferta. Recibiste %.2f %s."
                            .format(
                                execution.amountTaken,
                                toCurrency.codigoIso.trim(),
                                execution.subtotal,
                                fromCurrency.codigoIso.trim()
                            ),
                        "estadoenvio" to "Pendiente",
                        "fechacreacion" to now,
                        "fechaenvio" to null,
                        "referenciatipo" to "operacionesinmediatas",
                        "referenciaid" to operation.operacionInmediataId
                    )
                )

                api.insertHistorial(
                    mapOf(
                        "usuarioid" to offer.usuarioId,
                        "tipooperacion" to "Venta inmediata",
                        "referenciaid" to operation.operacionInmediataId,
                        "parmonedaid" to pair.parMonedaId,
                        "monedaid" to null,
                        "fechahora" to now,
                        "estado" to "Completada",
                        "metodoejecucion" to "Normal"
                    )
                )
            }

            api.insertHistorial(
                mapOf(
                    "usuarioid" to usuarioId,
                    "tipooperacion" to "Compra inmediata",
                    "referenciaid" to operation.operacionInmediataId,
                    "parmonedaid" to pair.parMonedaId,
                    "monedaid" to null,
                    "fechahora" to now,
                    "estado" to "Completada",
                    "metodoejecucion" to "Normal"
                )
            )

            AppSession.notifyWalletChanged()

            InstantBuyReceipt(
                operationId = operation.operacionInmediataId,
                boughtAmount = covered,
                paidTotal = total,
                fromCurrency = fromCurrency.codigoIso.trim(),
                toCurrency = toCurrency.codigoIso.trim()
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

            AppSession.notifyWalletChanged()

            "Operación cancelada. Se reembolsaron %.2f %s."
                .format(item.refundAmount, item.refundCurrency)
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

        return api.getMonedaByCode("eq.$cleaned").firstOrNull()
            ?: api.getMonedas().firstOrNull {
                it.codigoIso.trim().equals(cleaned, ignoreCase = true)
            }
            ?: error("No existe la moneda $cleaned en Supabase.")
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
        return api.getParMoneda(
            monedaOrigenId = "eq.$originCurrencyId",
            monedaDestinoId = "eq.$destinationCurrencyId"
        ).firstOrNull()
            ?: error("No existe el par de monedas seleccionado en Supabase.")
    }

    private suspend fun getAvailableOffers(
        pairId: Int,
        buyerId: Int
    ): List<OfertaVentaDto> {
        return api.getOfertasVentaByPair("eq.$pairId")
            .filter { it.usuarioId != buyerId }
            .filter { it.cantidadPendiente > 0.0 }
            .filter { isActiveStatus(it.estado) }
            .sortedWith(
                compareBy<OfertaVentaDto> { it.precioUnitario }
                    .thenBy { it.fechaCreacion ?: "" }
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

    suspend fun loadHomeData(usuarioId: Int): HomeData {
        // 1. Obtener todos los pares activos
        val pares = api.getAllParesMoneda()

        // 2. Para cada par, sumar volumen total del histórico
        data class ParVolumen(val parMonedaId: Int, val volumenTotal: Double)

        val volumenes = pares.map { par ->
            val historico = api.getHistoricoByPar("eq.${par.parMonedaId}", limit = 100)
            val volTotal = historico.sumOf { it.volumenCompra + it.volumenVenta }
            ParVolumen(par.parMonedaId, volTotal)
        }.sortedByDescending { it.volumenTotal }

        // 3. Par más activo global
        val globalParId = volumenes.firstOrNull()?.parMonedaId
        val globalChartData = if (globalParId != null) {
            buildChartData(globalParId)
        } else null

        // 4. Par más operado por el usuario
        val userOrders = api.getOrdenesCompraByUser("eq.$usuarioId")
        val userOffers = api.getOfertasVentaByUser("eq.$usuarioId")

        val userParCounts = mutableMapOf<Int, Int>()
        userOrders.forEach { userParCounts[it.parMonedaId] = (userParCounts[it.parMonedaId] ?: 0) + 1 }
        userOffers.forEach { userParCounts[it.parMonedaId] = (userParCounts[it.parMonedaId] ?: 0) + 1 }

        val userTopParId = userParCounts.maxByOrNull { it.value }?.key
        val userChartData = if (userTopParId != null) {
            buildChartData(userTopParId)
        } else globalChartData // fallback al global si no tiene operaciones

        return HomeData(
            globalMostActive = globalChartData,
            userMostActive = userChartData,
            hasUserActivity = userTopParId != null
        )
    }

    private suspend fun buildChartData(parMonedaId: Int): com.example.exchangededivisas.data.model.CurrencyPairChartData {
        val par = api.getParMonedaById("eq.$parMonedaId").firstOrNull()
            ?: return com.example.exchangededivisas.data.model.CurrencyPairChartData("?", "?", emptyList())

        val fromCode = api.getMonedaById("eq.${par.monedaOrigenId}")
            .firstOrNull()?.codigoIso?.trim() ?: "?"
        val toCode = api.getMonedaById("eq.${par.monedaDestinoId}")
            .firstOrNull()?.codigoIso?.trim() ?: "?"

        val historico = api.getHistoricoByPar(
            parMonedaId = "eq.$parMonedaId",
            order = "fecharegistro.asc",
            limit = 50
        )

        val prices = historico.mapNotNull { h ->
            val buy = h.mayorPrecioCompra ?: return@mapNotNull null
            val sell = h.menorPrecioVenta ?: return@mapNotNull null
            val fecha = h.fechaRegistro ?: return@mapNotNull null
            try {
                val dt = java.time.OffsetDateTime.parse(fecha).toLocalDateTime()
                com.example.exchangededivisas.data.model.HistoricalPrice(dt, buy, sell)
            } catch (e: Exception) { null }
        }

        return com.example.exchangededivisas.data.model.CurrencyPairChartData(fromCode, toCode, prices)
    }

    data class HomeData(
        val globalMostActive: com.example.exchangededivisas.data.model.CurrencyPairChartData?,
        val userMostActive: com.example.exchangededivisas.data.model.CurrencyPairChartData?,
        val hasUserActivity: Boolean
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

    /** Carga todos los pares activos con sus precios reales de Supabase */
    suspend fun getAllPairs(): Result<List<PairUi>> {
        return runCatching {
            val monedas = api.getMonedas().associateBy { it.monedaId }
            val pares   = api.getAllParesMoneda()

            pares.mapNotNull { par ->
                val from = monedas[par.monedaOrigenId]  ?: return@mapNotNull null
                val to   = monedas[par.monedaDestinoId] ?: return@mapNotNull null

                val offers = runCatching {
                    api.getOfertasVentaByPair("eq.${par.parMonedaId}")
                        .filter { isActiveStatus(it.estado) }
                }.getOrElse { emptyList() }

                // También intentamos historial para tener precio de compra real
                val historico = runCatching {
                    api.getHistoricoByPar("eq.${par.parMonedaId}", limit = 1)
                }.getOrElse { emptyList() }

                val bestSell = offers.minByOrNull { it.precioUnitario }?.precioUnitario
                    ?: historico.firstOrNull()?.menorPrecioVenta
                val bestBuy  = historico.firstOrNull()?.mayorPrecioCompra
                    ?: bestSell?.let { it * 0.99 }
                val volume   = offers.sumOf { it.cantidadPendiente }
                val margin   = if (bestBuy != null && bestSell != null) bestSell - bestBuy else null

                PairUi(
                    parMonedaId = par.parMonedaId,
                    fromCode    = from.codigoIso.trim(),
                    toCode      = to.codigoIso.trim(),
                    fromName    = from.nombre,
                    toName      = to.nombre,
                    bestBuy     = bestBuy,
                    bestSell    = bestSell,
                    margin      = margin,
                    volume      = volume
                )
            }
        }
    }

    /** Historial de precios real de un par para el gráfico */
    suspend fun getPairChartData(
        pairCode: String
    ): Result<com.example.exchangededivisas.data.model.CurrencyPairChartData> {
        return runCatching {
            val (fromCode, toCode) = parsePairCode(pairCode)
            val from = getCurrencyByCode(fromCode)
            val to   = getCurrencyByCode(toCode)
            val pair = getPairOrThrow(from.monedaId, to.monedaId)
            buildChartData(pair.parMonedaId)
        }
    }

    /** Ofertas de venta activas de un par (libro de órdenes) */
    suspend fun getActiveSellOffers(pairCode: String): Result<List<SellOfferUi>> {
        return runCatching {
            val (fromCode, toCode) = parsePairCode(pairCode)
            val from = getCurrencyByCode(fromCode)
            val to   = getCurrencyByCode(toCode)
            val pair = getPairOrThrow(from.monedaId, to.monedaId)

            api.getOfertasVentaByPair("eq.${pair.parMonedaId}")
                .filter { isActiveStatus(it.estado) }
                .sortedBy { it.precioUnitario }
                .map {
                    SellOfferUi(
                        ofertaId = it.ofertaVentaId,
                        quantity = it.cantidadPendiente,
                        price    = it.precioUnitario,
                        total    = it.cantidadPendiente * it.precioUnitario
                    )
                }
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

            val (fromCode, toCode) = parsePairCode(pairCode)
            val from = getCurrencyByCode(fromCode)
            val to   = getCurrencyByCode(toCode)
            val pair = getPairOrThrow(from.monedaId, to.monedaId)

            val available = getAvailableBalance(usuarioId, from.monedaId)
            require(available >= amount) { "Saldo insuficiente" }

            val now = nowIso()
            val (before, after) = subtractBalance(usuarioId, from.monedaId, amount)

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
                usuarioId, from.monedaId, "OfertaVenta", amount,
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
            api.insertNotificacionCorreo(mapOf(
                "usuarioid"         to usuarioId,
                "tiponotificacionid" to null,
                "correodestino"     to correo,
                "tipoevento"        to "OFERTA_CREADA",
                "asunto"            to "Tu oferta de venta fue registrada",
                "cuerpo"            to "Oferta de %.4f %s a precio %.4f %s creada correctamente."
                    .format(amount, from.codigoIso.trim(), price, to.codigoIso.trim()),
                "estadoenvio"       to "Pendiente",
                "fechacreacion"     to now,
                "fechaenvio"        to null,
                "referenciatipo"    to "ofertasventa",
                "referenciaid"      to oferta.ofertaVentaId
            ))

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
            val (baseCode, quoteCode) = parsePairCode(pairCode)
            val base  = getCurrencyByCode(baseCode)
            val quote = getCurrencyByCode(quoteCode)
            val pair  = getPairOrThrow(base.monedaId, quote.monedaId)

            // Para venta inmediata el usuario entrega BASE y recibe QUOTE
            // Consume las órdenes de compra activas del mercado que mejor paguen
            val offers = api.getOfertasVentaByPair("eq.${pair.parMonedaId}")
                .filter { isActiveStatus(it.estado) && it.usuarioId != usuarioId }
                .sortedByDescending { it.precioUnitario }

            val planned      = planOfferExecutions(offers, amount)
            val covered      = planned.sumOf { it.amountTaken }
            val totalReceive = planned.sumOf { it.subtotal }
            val prices       = planned.map { it.offer.precioUnitario }
            val available    = getAvailableBalance(usuarioId, base.monedaId)

            InstantSalePreview(
                pairCode         = "${base.codigoIso.trim()}_${quote.codigoIso.trim()}",
                baseCurrency     = base.codigoIso.trim(),
                quoteCurrency    = quote.codigoIso.trim(),
                requestedAmount  = amount,
                coveredAmount    = covered,
                totalToReceive   = totalReceive,
                minPrice         = prices.minOrNull() ?: 0.0,
                maxPrice         = prices.maxOrNull() ?: 0.0,
                avgPrice         = if (covered > 0.0) totalReceive / covered else 0.0,
                availableBalance = available,
                hasLiquidity     = covered >= amount,
                hasEnoughBalance = available >= amount
            )
        }
    }

    /** Ejecuta la venta inmediata contra las ofertas activas del mercado */
    suspend fun executeInstantSale(
        usuarioId: Int,
        pairCode: String,
        amount: Double
    ): Result<InstantSaleReceipt> {
        return runCatching {
            require(amount > 0.0) { "Valor inválido" }
            val (baseCode, quoteCode) = parsePairCode(pairCode)
            val base  = getCurrencyByCode(baseCode)
            val quote = getCurrencyByCode(quoteCode)
            val pair  = getPairOrThrow(base.monedaId, quote.monedaId)

            val offers = api.getOfertasVentaByPair("eq.${pair.parMonedaId}")
                .filter { isActiveStatus(it.estado) && it.usuarioId != usuarioId }
                .sortedByDescending { it.precioUnitario }

            val planned      = planOfferExecutions(offers, amount)
            val covered      = planned.sumOf { it.amountTaken }
            require(covered >= amount) { "Liquidez insuficiente" }

            val totalReceive = planned.sumOf { it.subtotal }
            val available    = getAvailableBalance(usuarioId, base.monedaId)
            require(available >= amount) { "Saldo insuficiente" }

            val prices = planned.map { it.offer.precioUnitario }
            val now    = nowIso()

            val operation = api.insertOperacionInmediata(mapOf(
                "usuarioid"         to usuarioId,
                "parmonedaid"       to pair.parMonedaId,
                "tipooperacion"     to "Venta inmediata",
                "metodoejecucion"   to "Normal",
                "cantidadsolicitada" to amount,
                "cantidadejecutada" to covered,
                "preciominimo"      to (prices.minOrNull() ?: 0.0),
                "preciomaximo"      to (prices.maxOrNull() ?: 0.0),
                "preciopromedio"    to if (covered > 0.0) totalReceive / covered else 0.0,
                "totalpagado"       to amount,
                "totalrecibido"     to totalReceive,
                "estado"            to "Completada",
                "fechaoperacion"    to now,
                "operacionpadreid"  to null
            )).first()

            val (sbBefore, sbAfter) = subtractBalance(usuarioId, base.monedaId, amount)
            insertWalletMovement(usuarioId, base.monedaId, "VentaInmediata", amount,
                sbBefore, sbAfter, "operacionesinmediatas", operation.operacionInmediataId, now)

            val (sqBefore, sqAfter) = addBalance(usuarioId, quote.monedaId, totalReceive)
            insertWalletMovement(usuarioId, quote.monedaId, "VentaInmediata", totalReceive,
                sqBefore, sqAfter, "operacionesinmediatas", operation.operacionInmediataId, now)

            planned.forEach { execution ->
                val offer      = execution.offer
                val newPending = max(0.0, offer.cantidadPendiente - execution.amountTaken)
                api.updateOfertaVenta("eq.${offer.ofertaVentaId}", mapOf(
                    "cantidadvendida"    to offer.cantidadVendida + execution.amountTaken,
                    "cantidadpendiente"  to newPending,
                    "estado"             to if (newPending <= 0.000001) "Completada" else "Parcialmente ejecutada",
                    "fechaactualizacion" to now
                ))
                val buyerEmail = api.getUsuarioById("eq.${offer.usuarioId}").firstOrNull()
                    ?.correoElectronico ?: "correo@pendiente.com"
                api.insertNotificacionCorreo(mapOf(
                    "usuarioid" to offer.usuarioId, "tiponotificacionid" to null,
                    "correodestino" to buyerEmail, "tipoevento" to "PROGRESO_OFERTA",
                    "asunto" to "Tu oferta recibio una venta",
                    "cuerpo" to "Se ejecutaron %.2f %s de tu oferta."
                        .format(execution.amountTaken, base.codigoIso.trim()),
                    "estadoenvio" to "Pendiente", "fechacreacion" to now,
                    "fechaenvio" to null, "referenciatipo" to "operacionesinmediatas",
                    "referenciaid" to operation.operacionInmediataId
                ))
            }

            api.insertHistorial(mapOf(
                "usuarioid" to usuarioId, "tipooperacion" to "Venta inmediata",
                "referenciaid" to operation.operacionInmediataId,
                "parmonedaid" to pair.parMonedaId, "monedaid" to null,
                "fechahora" to now, "estado" to "Completada", "metodoejecucion" to "Normal"
            ))

            AppSession.notifyWalletChanged()
            InstantSaleReceipt(operation.operacionInmediataId, covered, totalReceive,
                base.codigoIso.trim(), quote.codigoIso.trim())
        }
    }

    /** Ejecuta retiro descontando saldo real de la billetera */
    suspend fun executeWithdraw(
        usuarioId: Int,
        items: List<WithdrawItem>,
        metodoPagoId: Int,
        comisionPorcentaje: Double
    ): Result<WithdrawReceipt> {
        return runCatching {
            require(items.isNotEmpty()) { "Selecciona al menos una moneda" }
            items.forEach { require(it.amount > 0.0) { "Monto invalido para ${it.code}" } }

            val now      = nowIso()
            val subtotal = items.sumOf { it.amount }
            val commission = subtotal * comisionPorcentaje
            val net      = subtotal - commission

            items.forEach { item ->
                val (before, after) = subtractBalance(usuarioId, item.monedaId, item.amount)
                val deposito = api.insertDeposito(mapOf(
                    "usuarioid"        to usuarioId,
                    "monedaid"         to item.monedaId,
                    "metodopagoid"     to metodoPagoId,
                    "montodepositado"  to item.amount,
                    "comisionaplicada" to (item.amount * comisionPorcentaje),
                    "montoneto"        to (item.amount * (1 - comisionPorcentaje)),
                    "estado"           to "Completado",
                    "fechadeposito"    to now,
                    "fechaactualizacion" to now
                )).first()

                insertWalletMovement(usuarioId, item.monedaId, "Retiro", item.amount,
                    before, after, "depositos", deposito.depositoId, now)

                api.insertHistorial(mapOf(
                    "usuarioid" to usuarioId, "tipooperacion" to "Retiro",
                    "referenciaid" to deposito.depositoId, "parmonedaid" to null,
                    "monedaid" to item.monedaId, "fechahora" to now,
                    "estado" to "Completado", "metodoejecucion" to "Normal"
                ))
            }

            AppSession.notifyWalletChanged()
            WithdrawReceipt(items.associate { it.code to it.amount }, commission, net)
        }
    }
}