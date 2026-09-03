package com.tangem.features.polymarket.impl.placeprediction.model.converter

import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import java.math.BigDecimal

internal object PlaceResultUMConverter {

    private const val MATCHED_STATUS = "matched"

    private val FILL_SLACK = BigDecimal.ONE.movePointLeft(PolymarketContracts.COLLATERAL_DECIMALS)

    fun convert(result: PredictionPlaceOrderResult, requestedAmount: BigDecimal, placedAt: Long): PlaceResultUM {
        val isMatched = result.status?.trim()?.equals(MATCHED_STATUS, ignoreCase = true) == true
        val filled = result.makingAmount

        if (!isMatched || filled == null) return PlaceResultUM.Submitted(placedAt = placedAt)

        return when {
            filled.signum() <= 0 -> PlaceResultUM.NotFilled(placedAt = placedAt)
            (requestedAmount - filled).abs() <= FILL_SLACK -> PlaceResultUM.Filled(
                placedAt = placedAt,
                amount = requestedAmount,
                tokensBought = result.takingAmount,
            )
            filled > requestedAmount -> PlaceResultUM.Submitted(placedAt = placedAt)
            else -> PlaceResultUM.PartiallyFilled(
                placedAt = placedAt,
                filledAmount = filled,
                requestedAmount = requestedAmount,
                tokensBought = result.takingAmount,
            )
        }
    }
}