package com.tangem.features.polymarket.impl.placeprediction.model.converter

import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal sealed interface PlaceOutcome {

    data class Reportable(val result: PlaceResultUM) : PlaceOutcome

    data class Anomalous(val reason: String) : PlaceOutcome

    data class Delayed(val orderId: String, val placedAt: Long) : PlaceOutcome
}

internal object PlaceResultUMConverter {

    private const val MATCHED_STATUS = "matched"

    private const val DELAYED_STATUS = "delayed"

    private const val FIAT_SCALE = 2

    private const val ZERO_SHARES_REASON = "The exchange matched the order and filled no shares"

    private const val UNREADABLE_FILL_REASON = "The exchange matched the order, but its fill cannot be read"

    /**
     * @param side the side the order was signed for — it decides which leg of the response is the shares one
     * @param quote the signed quote — its shares leg is the worst case the order was signed for, so a fill
     *  reaching it is a full one and only a fill short of it is partial; its collateral leg, fee and total are
     *  shown to the user but never used to decide the fill
     */
    fun convert(
        result: PredictionPlaceOrderResult,
        side: PredictionOrderSide,
        quote: PredictionOrderQuote,
        placedAt: Long,
    ): PlaceOutcome {
        val status = result.status?.trim()
        if (status.equals(DELAYED_STATUS, ignoreCase = true)) {
            val orderId = result.orderId?.takeIf { it.isNotBlank() } ?: return submitted(placedAt = placedAt)
            return PlaceOutcome.Delayed(orderId = orderId, placedAt = placedAt)
        }

        val isMatched = status.equals(MATCHED_STATUS, ignoreCase = true)
        val filledShares = result.sharesLeg(side = side)

        if (!isMatched || filledShares == null) return submitted(placedAt = placedAt)

        return when {
            filledShares.signum() <= 0 -> PlaceOutcome.Anomalous(reason = ZERO_SHARES_REASON)
            filledShares >= quote.shares -> result.collateralLeg(side = side)
                ?.let { amount ->
                    PlaceOutcome.Reportable(
                        result = PlaceResultUM.Filled(
                            placedAt = placedAt,
                            collateralAmount = amount,
                            shares = filledShares,
                            fee = quote.fees.total,
                            total = totalOf(side = side, collateral = amount, fee = quote.fees.total),
                        ),
                    )
                }
                ?: submitted(placedAt = placedAt)
            else -> result.collateralLeg(side = side)
                ?.let { spent ->
                    PlaceOutcome.Reportable(
                        result = PlaceResultUM.PartiallyFilled(
                            placedAt = placedAt,
                            filledCollateral = spent,
                            requestedCollateral = quote.notional,
                            shares = filledShares,
                            total = partialTotal(
                                side = side,
                                filledCollateral = spent,
                                filledShares = filledShares,
                                orderedShares = quote.shares,
                                quote = quote,
                            ),
                        ),
                    )
                }
                ?: submitted(placedAt = placedAt)
        }
    }

    private fun PredictionPlaceOrderResult.sharesLeg(side: PredictionOrderSide): BigDecimal? = when (side) {
        PredictionOrderSide.BUY -> takingAmount
        PredictionOrderSide.SELL -> makingAmount
    }

    private fun PredictionPlaceOrderResult.collateralLeg(side: PredictionOrderSide): BigDecimal? = when (side) {
        PredictionOrderSide.BUY -> makingAmount
        PredictionOrderSide.SELL -> takingAmount
    }

    /**
     * The fill of an order the book matched after its delay: the matched shares against the order's size, spent
     * at the order's price and rounded half up to cents. Anomalous when nothing filled or a size or the price
     * cannot be read, since then there is no fill to report.
     */
    fun convert(order: PredictionOrderState, quote: PredictionOrderQuote, placedAt: Long): PlaceOutcome {
        val filledShares = order.sizeMatched?.takeIf { it.signum() > 0 }
            ?: return PlaceOutcome.Anomalous(reason = ZERO_SHARES_REASON)
        val orderedShares = order.originalSize ?: return PlaceOutcome.Anomalous(reason = UNREADABLE_FILL_REASON)
        val price = order.price ?: return PlaceOutcome.Anomalous(reason = UNREADABLE_FILL_REASON)

        val filledAmount = filledShares * price
        val executedAmount = filledAmount.setScale(FIAT_SCALE, RoundingMode.HALF_UP)

        val result = if (filledShares >= orderedShares) {
            PlaceResultUM.Filled(
                placedAt = placedAt,
                collateralAmount = executedAmount,
                shares = filledShares,
                fee = quote.fees.total,
                total = totalOf(side = quote.side, collateral = filledAmount, fee = quote.fees.total),
            )
        } else {
            PlaceResultUM.PartiallyFilled(
                placedAt = placedAt,
                filledCollateral = executedAmount,
                requestedCollateral = quote.notional,
                shares = filledShares,
                total = partialTotal(
                    side = quote.side,
                    filledCollateral = filledAmount,
                    filledShares = filledShares,
                    orderedShares = orderedShares,
                    quote = quote,
                ),
            )
        }
        return PlaceOutcome.Reportable(result = result)
    }

    private fun partialTotal(
        side: PredictionOrderSide,
        filledCollateral: BigDecimal,
        filledShares: BigDecimal,
        orderedShares: BigDecimal,
        quote: PredictionOrderQuote,
    ): BigDecimal? {
        val filledRatio = fillRatio(
            filledCollateral = filledCollateral,
            filledShares = filledShares,
            orderedShares = orderedShares,
            quote = quote,
        ) ?: return null
        val filledFee = quote.fees.total.multiply(filledRatio, MathContext.DECIMAL64)
        return totalOf(side = side, collateral = filledCollateral, fee = filledFee)
    }

    private fun totalOf(side: PredictionOrderSide, collateral: BigDecimal, fee: BigDecimal): BigDecimal = when (side) {
        PredictionOrderSide.BUY -> (collateral + fee).setScale(FIAT_SCALE, RoundingMode.UP)
        PredictionOrderSide.SELL -> (collateral - fee)
            .setScale(FIAT_SCALE, RoundingMode.DOWN)
            .coerceAtLeast(BigDecimal.ZERO.setScale(FIAT_SCALE))
    }

    private fun fillRatio(
        filledCollateral: BigDecimal,
        filledShares: BigDecimal,
        orderedShares: BigDecimal,
        quote: PredictionOrderQuote,
    ) = when {
        orderedShares.signum() > 0 -> filledShares.divide(orderedShares, MathContext.DECIMAL64)
        quote.notional.signum() > 0 -> filledCollateral.divide(quote.notional, MathContext.DECIMAL64)
        else -> null
    }

    private fun submitted(placedAt: Long) = PlaceOutcome.Reportable(result = PlaceResultUM.Submitted(placedAt))
}