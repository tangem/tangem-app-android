package com.tangem.features.polymarket.impl.placeprediction.model.converter

import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import java.math.BigDecimal
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
     * @param quotedShares the shares leg of the signed quote — the worst case the order was signed for, so a
     *  fill reaching it is a full one and only a fill short of it is partial
     * @param quotedAmount the collateral leg, shown to the user but never used to decide the fill
     */
    fun convert(
        result: PredictionPlaceOrderResult,
        side: PredictionOrderSide,
        quotedShares: BigDecimal,
        quotedAmount: BigDecimal,
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
            filledShares >= quotedShares -> result.collateralLeg(side = side)
                ?.let { amount ->
                    PlaceOutcome.Reportable(
                        result = PlaceResultUM.Filled(
                            placedAt = placedAt,
                            collateralAmount = amount,
                            shares = filledShares,
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
                            requestedCollateral = quotedAmount,
                            shares = filledShares,
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
    fun convert(order: PredictionOrderState, quotedAmount: BigDecimal, placedAt: Long): PlaceOutcome {
        val filledShares = order.sizeMatched?.takeIf { it.signum() > 0 }
            ?: return PlaceOutcome.Anomalous(reason = ZERO_SHARES_REASON)
        val orderedShares = order.originalSize ?: return PlaceOutcome.Anomalous(reason = UNREADABLE_FILL_REASON)
        val price = order.price ?: return PlaceOutcome.Anomalous(reason = UNREADABLE_FILL_REASON)

        val executedAmount = (filledShares * price).setScale(FIAT_SCALE, RoundingMode.HALF_UP)

        val result = if (filledShares >= orderedShares) {
            PlaceResultUM.Filled(placedAt = placedAt, collateralAmount = executedAmount, shares = filledShares)
        } else {
            PlaceResultUM.PartiallyFilled(
                placedAt = placedAt,
                filledCollateral = executedAmount,
                requestedCollateral = quotedAmount,
                shares = filledShares,
            )
        }
        return PlaceOutcome.Reportable(result = result)
    }

    private fun submitted(placedAt: Long) = PlaceOutcome.Reportable(result = PlaceResultUM.Submitted(placedAt))
}