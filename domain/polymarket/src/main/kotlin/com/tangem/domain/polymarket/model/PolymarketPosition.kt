package com.tangem.domain.polymarket.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** An outcome holding or its closed history, scoped to a wallet by the requesting repository call. */
sealed interface PolymarketPosition {

    val id: PolymarketPositionId
    val title: String
    val iconUrl: String?
    val outcome: String

    /** Position of the outcome among the market's outcomes; `null` when upstream does not serve it. */
    val outcomeIndex: Int?
    val endDate: LocalDate?
    val avgPrice: BigDecimal
    val curPrice: BigDecimal
    val realizedPnl: BigDecimal

    /**
     * Shares still held, including resolved shares awaiting claim.
     * [cashPnl] and [percentPnl] describe only the remaining holding; [realizedPnl] covers earlier sales.
     * [currentValue] is an indicative mark, not guaranteed sale proceeds.
     */
    data class Open(
        override val id: PolymarketPositionId,
        override val title: String,
        override val iconUrl: String?,
        override val outcome: String,
        override val outcomeIndex: Int?,
        override val endDate: LocalDate?,
        override val avgPrice: BigDecimal,
        override val curPrice: BigDecimal,
        override val realizedPnl: BigDecimal,
        val marketId: String?,
        val eventId: String?,
        val eventTitle: String?,
        val size: BigDecimal,
        val initialValue: BigDecimal,
        val currentValue: BigDecimal,
        val cashPnl: BigDecimal,
        val percentPnl: BigDecimal,
        val totalBought: BigDecimal,
        val isRedeemable: Boolean,
        val isNegRisk: Boolean,
        val isAcceptingOrders: Boolean,
        val isLive: Boolean,
        val resolutionStatus: PolymarketResolutionStatus,
        val result: PolymarketPositionResult?,
    ) : PolymarketPosition

    /** History of sold-out or claimed shares. There is no remaining value or route to an event. */
    data class Closed(
        override val id: PolymarketPositionId,
        override val title: String,
        override val iconUrl: String?,
        override val outcome: String,
        override val outcomeIndex: Int?,
        override val endDate: LocalDate?,
        override val avgPrice: BigDecimal,
        override val curPrice: BigDecimal,
        override val realizedPnl: BigDecimal,
        val totalBought: BigDecimal,
        val closedAt: Instant,
        val status: PolymarketClosedPositionStatus,
    ) : PolymarketPosition
}