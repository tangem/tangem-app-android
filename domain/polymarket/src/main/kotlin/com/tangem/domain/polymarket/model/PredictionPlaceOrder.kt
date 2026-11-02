package com.tangem.domain.polymarket.model

import java.math.BigDecimal

data class PredictionSignedOrder(
    val order: PredictionOrder,
    val signature: String,
    val domain: PolymarketExchangeDomain,
)

data class PredictionPlaceOrderResult(
    val isSuccess: Boolean,
    val orderId: String?,
    val status: String?,
    val makingAmount: BigDecimal?,
    val takingAmount: BigDecimal?,
    val transactionHashes: List<String>,
    val errorMessage: String?,
)

sealed interface PredictionPlaceOrderError : PolymarketError {

    data class Rejected(val detail: String?) : PredictionPlaceOrderError

    data class NoLiquidity(val detail: String?) : PredictionPlaceOrderError

    data class Unauthorized(val detail: String?) : PredictionPlaceOrderError

    /**
     * @property httpCode a `4xx` other than `401` whose message names the signature, the signer, an authorization or
     * the API key; a bare `403`, a geoblock for one, is not a refusal
     * @property reason the CLOB message
     */
    data class SessionKeyRefused(val httpCode: Int, val reason: String) : PredictionPlaceOrderError

    data class NotSent(val detail: String?) : PredictionPlaceOrderError

    data class Ambiguous(val detail: String?) : PredictionPlaceOrderError
}