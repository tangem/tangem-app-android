package com.tangem.domain.polymarket.model

import java.math.BigDecimal
import java.time.Instant

data class PolymarketActivity(
    val id: String,
    val type: Type,
    val occurredAt: Instant,
    val amount: BigDecimal,
    val transactionHash: String? = null,
    val market: Market? = null,
    val order: Order? = null,
    val redeemResult: RedeemResult? = null,
) {

    enum class Type { BUY, SELL, REDEEM, DEPOSIT, WITHDRAW }

    enum class RedeemResult { WON, LOST, SPLIT }

    data class Market(
        val conditionId: String,
        val outcomeIndex: Int,
        val shares: BigDecimal,
        val outcome: String?,
        val marketId: String?,
        val slug: String?,
        val title: String?,
        val icon: String?,
        val image: String?,
        val endDate: Instant?,
        val isNegRisk: Boolean?,
        val event: Event?,
    )

    data class Order(
        val assetId: String,
        val averagePrice: BigDecimal,
        val platformFee: BigDecimal,
        val builderFee: BigDecimal,
        val notional: BigDecimal,
        val tradeCount: Int,
    )

    data class Event(
        val eventId: String,
        val slug: String,
        val title: String,
        val icon: String?,
        val image: String?,
        val endDate: Instant?,
    )
}

data class PolymarketActivityFilter(
    val type: PolymarketActivity.Type? = null,
    val redeemResult: PolymarketActivity.RedeemResult? = null,
)

data class PolymarketActivityPage(val items: List<PolymarketActivity>, val nextCursor: String?)

sealed interface PolymarketActivityError : PolymarketError {
    data object Network : PolymarketActivityError

    data class Unknown(val httpCode: Int?, val detail: String?) : PolymarketActivityError
}