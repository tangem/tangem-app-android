package com.tangem.domain.polymarket.model

import java.math.BigDecimal

data class PredictionSignedOrder(
    val order: PredictionOrder,
    val signature: String,
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

    data class NotSent(val detail: String?) : PredictionPlaceOrderError

    data class Ambiguous(val detail: String?) : PredictionPlaceOrderError
}