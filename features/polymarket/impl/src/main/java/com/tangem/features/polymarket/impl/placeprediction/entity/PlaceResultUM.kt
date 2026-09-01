package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * Terminal outcome of a placed order.
 *
 * A technical failure — the order never reached the book — is deliberately not a case here: it is shown as a
 * dialog over the summary and returns the flow to [SubmitUM.Idle], so the user can retry the same order.
 */
@Immutable
internal sealed interface PlaceResultUM {

    val placedAt: Long

    data class Filled(override val placedAt: Long, val amount: BigDecimal) : PlaceResultUM

    data class PartiallyFilled(
        override val placedAt: Long,
        val filledAmount: BigDecimal,
        val requestedAmount: BigDecimal,
    ) : PlaceResultUM

    data class NotFilled(override val placedAt: Long) : PlaceResultUM
}