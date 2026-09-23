package com.tangem.features.polymarket.impl.main.model

internal enum class PredictionBalanceMode {
    AVAILABLE,
    TOTAL,
    ;

    fun toggled(): PredictionBalanceMode = when (this) {
        AVAILABLE -> TOTAL
        TOTAL -> AVAILABLE
    }
}