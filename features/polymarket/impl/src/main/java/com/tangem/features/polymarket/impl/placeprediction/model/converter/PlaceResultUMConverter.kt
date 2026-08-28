package com.tangem.features.polymarket.impl.placeprediction.model.converter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import java.math.BigDecimal

internal object PlaceResultUMConverter {

    private const val MATCHED_STATUS = "matched"

    private val FILL_SLACK = BigDecimal.ONE.movePointLeft(PolymarketContracts.COLLATERAL_DECIMALS)

    fun convert(
        result: PredictionPlaceOrderResult,
        requestedAmount: BigDecimal,
        placedAtMillis: Long,
    ): Either<PredictionPlaceOrderError, PlaceResultUM> {
        if (!result.isSuccess) return PredictionPlaceOrderError.Rejected(detail = result.errorMessage).left()

        val isMatched = result.status?.trim()?.equals(MATCHED_STATUS, ignoreCase = true) == true
        val filled = result.makingAmount

        if (!isMatched || filled == null) return PlaceResultUM.Submitted(placedAt = placedAtMillis).right()

        return when {
            filled.signum() <= 0 -> PlaceResultUM.NotFilled(placedAt = placedAtMillis)
            (requestedAmount - filled).abs() <= FILL_SLACK -> PlaceResultUM.Filled(
                placedAt = placedAtMillis,
                amount = requestedAmount,
            )
            filled > requestedAmount -> PlaceResultUM.Submitted(placedAt = placedAtMillis)
            else -> PlaceResultUM.PartiallyFilled(
                placedAt = placedAtMillis,
                filledAmount = filled,
                requestedAmount = requestedAmount,
            )
        }.right()
    }
}