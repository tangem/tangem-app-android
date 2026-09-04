package com.tangem.features.polymarket.impl.placeprediction.model.converter

import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import java.math.BigDecimal

internal sealed interface PlaceOutcome {

    data class Reportable(val result: PlaceResultUM) : PlaceOutcome

    data class Anomalous(val reason: String) : PlaceOutcome
}

internal object PlaceResultUMConverter {

    private const val MATCHED_STATUS = "matched"

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
        val isMatched = result.status?.trim()?.equals(MATCHED_STATUS, ignoreCase = true) == true
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

    private fun submitted(placedAt: Long) = PlaceOutcome.Reportable(result = PlaceResultUM.Submitted(placedAt))
}