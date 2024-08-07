package com.tangem.features.markets.details.impl.ui.entity

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable

@Immutable
internal data class SecurityScoreUM(
    @FloatRange(from = 0.0, to = 5.0) val score: Float,
    val description: String,
    val onInfoClick: () -> Unit,
)
