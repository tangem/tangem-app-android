package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

@Immutable
internal sealed interface PlaceResultUM {

    val placedAt: Long

    data class Filled(
        override val placedAt: Long,
        val collateralAmount: BigDecimal,
        val shares: BigDecimal?,
    ) : PlaceResultUM

    data class PartiallyFilled(
        override val placedAt: Long,
        val filledCollateral: BigDecimal,
        val requestedCollateral: BigDecimal,
        val shares: BigDecimal?,
    ) : PlaceResultUM

    data class NotFilled(override val placedAt: Long) : PlaceResultUM

    data class Submitted(override val placedAt: Long) : PlaceResultUM
}