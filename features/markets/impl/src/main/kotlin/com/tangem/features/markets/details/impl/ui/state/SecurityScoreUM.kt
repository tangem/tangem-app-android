package com.tangem.features.markets.details.impl.ui.state

import androidx.annotation.FloatRange

internal data class SecurityScoreUM(
    @FloatRange(from = 0.0, to = 5.0) val score: Float,
    val description: String,
    val onInfoClick: () -> Unit,
)
