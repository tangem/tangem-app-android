package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

@Immutable
internal sealed interface PlaceResultUM {

    val placedAt: Long

    data class Filled(
        override val placedAt: Long,
        val amount: BigDecimal,
        val tokensBought: BigDecimal?,
    ) : PlaceResultUM

    data class PartiallyFilled(
        override val placedAt: Long,
        val filledAmount: BigDecimal,
        val requestedAmount: BigDecimal,
        val tokensBought: BigDecimal?,
    ) : PlaceResultUM

    data class NotFilled(override val placedAt: Long) : PlaceResultUM

    data class Submitted(override val placedAt: Long) : PlaceResultUM
}