package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface PredictionNotificationUM {

    val isBlocking: Boolean

    data object PartialFill : PredictionNotificationUM {
        override val isBlocking = false
    }

    data object NoLiquidity : PredictionNotificationUM {
        override val isBlocking = true
    }

    data object InsufficientBalance : PredictionNotificationUM {
        override val isBlocking = true
    }

    data object BelowMinOrderSize : PredictionNotificationUM {
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