package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface PredictionNotificationUM {

    val isBlocking: Boolean

    data object AwaitingFunds : PredictionNotificationUM {
        override val isBlocking = false
    }

    data object PartialFill : PredictionNotificationUM {
        override val isBlocking = false
    }

    data object NoLiquidity : PredictionNotificationUM {
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

    data object PriceFailed : PredictionNotificationUM {
        override val isBlocking = true
    }

    data object PositionUnavailable : PredictionNotificationUM {
        override val isBlocking = true
    }
}