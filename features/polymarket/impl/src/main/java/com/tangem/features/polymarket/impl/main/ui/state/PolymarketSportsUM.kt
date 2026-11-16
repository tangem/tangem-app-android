package com.tangem.features.polymarket.impl.main.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class PolymarketSportsUM(
    val home: Team?,
    val away: Team?,
    val homeScore: String?,
    val awayScore: String?,
    val period: String?,
    val isLive: Boolean,
) {
    @Immutable
    data class Team(val name: TextReference, val logoUrl: String?)
}