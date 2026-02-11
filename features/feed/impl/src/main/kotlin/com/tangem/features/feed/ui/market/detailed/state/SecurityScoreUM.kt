package com.tangem.features.feed.ui.market.detailed.state

import androidx.annotation.FloatRange
import com.tangem.core.ui.extensions.TextReference

internal data class SecurityScoreUM(
    @FloatRange(from = 0.0, to = 5.0) val score: Float,
    val description: TextReference,
    val onInfoClick: () -> Unit,
)