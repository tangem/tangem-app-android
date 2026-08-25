package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * Notifications the flow can raise.
 *
 * [isBlocking] is what decides whether a notification stops the order: a warning that should not stop it
 * declares itself non-blocking, instead of being special-cased at the button. The button also needs a
 * priced quote, an idle submission and a sufficient balance — see `recomputeGate`.
 */
@Immutable
internal sealed interface PredictionNotificationUM {

    val isBlocking: Boolean

    data object PartialFill : PredictionNotificationUM {
        override val isBlocking = false
    }

    data object NoLiquidity : PredictionNotificationUM {
        override val isBlocking = false
    }

    /** The balance does not cover what the order would debit, fees included. */
    data object InsufficientBalance : PredictionNotificationUM {
        override val isBlocking = true
    }

    data class BelowMinOrderSize(val minOrderSize: BigDecimal) : PredictionNotificationUM {
        override val isBlocking = true
    }

    data object MarketClosed : PredictionNotificationUM {
        override val isBlocking = true
    }

    data object RegionRestricted : PredictionNotificationUM {
        override val isBlocking = true
    }

    data object QuoteFailed : PredictionNotificationUM {
        override val isBlocking = true
    }
}