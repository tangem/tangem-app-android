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
     * at the order's price and rounded half up to cents. Null when a size or the price cannot be read, since
     * then there is no fill to report.
     */
    fun convert(order: PredictionOrderState, quotedAmount: BigDecimal, placedAt: Long): PlaceResultUM? {
        val filledShares = order.sizeMatched ?: return null
        val orderedShares = order.originalSize ?: return null
        val price = order.price ?: return null

        if (filledShares.signum() <= 0) return PlaceResultUM.NotFilled(placedAt = placedAt)

        val executedAmount = (filledShares * price).setScale(FIAT_SCALE, RoundingMode.HALF_UP)

        return if (filledShares >= orderedShares) {
            PlaceResultUM.Filled(placedAt = placedAt, collateralAmount = executedAmount, shares = filledShares)
        } else {
            PlaceResultUM.PartiallyFilled(
                placedAt = placedAt,
                filledCollateral = executedAmount,
                requestedCollateral = quotedAmount,
                shares = filledShares,
            )
        }
    }

    private fun submitted(placedAt: Long) = PlaceOutcome.Reportable(result = PlaceResultUM.Submitted(placedAt))
}