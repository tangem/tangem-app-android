package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * What the user asked to be quoted.
 *
 * @property amount for [PredictionOrderSide.BUY] — collateral to spend; for [PredictionOrderSide.SELL] —
 *  shares to sell.
 * @property slippagePercent tolerance in percent, `null` to let the BFF apply its default.
 */
data class PredictionOrderQuoteRequest(
    val marketId: String,
    val assetId: String,
    val side: PredictionOrderSide,
    val amount: BigDecimal,
    val slippagePercent: BigDecimal?,
)

/**
 * Preview of a fill-and-kill order against the live book.
 *
 * The two legs are asymmetric by side: on a BUY, [shares] are guaranteed at [worstCasePrice] while
 * [expectedExecutionAmount] is what the average price would actually buy; on a SELL, the legs swap.
 *
 * @property total what is debited (BUY) or credited (SELL) once fees are applied.
 * @property minOrderSize the market's floor **in shares**, not in collateral: the sum it forbids is
 *  `minOrderSize × worstCasePrice`. It stays populated even in the states that zero everything else, and
 *  it does not agree with the minimum the exchange itself enforces — nothing is gated on it yet.
 * @property builderCode the code the order must carry for the builder fee to be attributed.
 * @property isLive whether the market's event is running right now.
 */
data class PredictionOrderQuote(
    val status: PredictionQuoteStatus,
    val shares: BigDecimal,
    val notional: BigDecimal,
    val expectedExecutionAmount: BigDecimal,
    val averagePrice: BigDecimal,
    val worstCasePrice: BigDecimal,
    val fees: PredictionOrderFees,
    val total: BigDecimal,
    val builderCode: String,
    val minOrderSize: BigDecimal,
    val tickSize: BigDecimal,
    val isLive: Boolean,
)

data class PredictionOrderFees(
    val market: BigDecimal,
    val builder: BigDecimal,
    val total: BigDecimal,
)