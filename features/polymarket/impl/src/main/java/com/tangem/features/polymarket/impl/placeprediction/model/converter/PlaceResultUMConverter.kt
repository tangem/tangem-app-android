package com.tangem.features.polymarket.impl.placeprediction.model.converter

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
     * @param quotedShares the shares leg of the signed quote — the leg a fill is measured against, since the
     *  collateral leg is truncated by the exchange whenever the implied price is not exactly representable
     * @param quotedAmount the collateral leg, shown to the user but never used to decide the fill
     */
    fun convert(
        result: PredictionPlaceOrderResult,
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
        val filledShares = result.takingAmount

        if (!isMatched || filledShares == null) return submitted(placedAt = placedAt)

        return when {
            filledShares.signum() <= 0 -> PlaceOutcome.Anomalous(reason = ZERO_SHARES_REASON)
            filledShares.compareTo(quotedShares) == 0 -> PlaceOutcome.Reportable(
                result = PlaceResultUM.Filled(placedAt = placedAt, amount = quotedAmount, tokensBought = filledShares),
            )
            filledShares > quotedShares -> submitted(placedAt = placedAt)
            else -> result.makingAmount
                ?.let { spent ->
                    PlaceOutcome.Reportable(
                        result = PlaceResultUM.PartiallyFilled(
                            placedAt = placedAt,
                            filledAmount = spent,
                            requestedAmount = quotedAmount,
                            tokensBought = filledShares,
                        ),
                    )
                }
                ?: submitted(placedAt = placedAt)
        }
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
            PlaceResultUM.Filled(placedAt = placedAt, amount = executedAmount, tokensBought = filledShares)
        } else {
            PlaceResultUM.PartiallyFilled(
                placedAt = placedAt,
                filledAmount = executedAmount,
                requestedAmount = quotedAmount,
                tokensBought = filledShares,
            )
        }
    }

    private fun submitted(placedAt: Long) = PlaceOutcome.Reportable(result = PlaceResultUM.Submitted(placedAt))
}