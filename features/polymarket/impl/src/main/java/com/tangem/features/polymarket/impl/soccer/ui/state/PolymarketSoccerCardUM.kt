package com.tangem.features.polymarket.impl.soccer.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PolymarketSoccerCardUM(
    val id: String,
    val title: TextReference,
    val iconUrl: String?,
    val volume: TextReference,
    val rules: Rules?,
    val content: Content,
) {
    @Immutable
    data class Rules(val title: TextReference, val description: String)

    @Immutable
    sealed interface Content {
        data class Buttons(val outcomes: ImmutableList<Outcome>) : Content
        data class Rows(val outcomes: ImmutableList<Outcome>) : Content
    }

    @Immutable
    data class Outcome(
        val target: Target,
        val title: TextReference,
        val price: TextReference,
        val appearance: PolymarketOutcomeAccent,
    )

    /** The owning child event is needed by the existing order flow to reload the selected market. */
    @Immutable
    data class Target(val eventId: String, val marketId: String, val assetId: String)
}