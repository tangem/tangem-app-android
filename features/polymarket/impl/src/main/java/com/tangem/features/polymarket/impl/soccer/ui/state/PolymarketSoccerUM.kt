package com.tangem.features.polymarket.impl.soccer.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.polymarket.impl.common.PolymarketResolutionSourceUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketSportsUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface PolymarketSoccerUM {
    data object Loading : PolymarketSoccerUM
    data object Error : PolymarketSoccerUM

    @Suppress("LongParameterList")
    data class Content(
        val title: TextReference,
        val league: TextReference,
        val kickoff: TextReference?,
        val volume: TextReference,
        val change24h: PriceChangeState,
        val sports: PolymarketSportsUM,
        val tabs: ImmutableList<TangemTabItemUM>,
        val cards: ImmutableList<PolymarketSoccerCardUM>,
        val description: TextReference?,
        val isDescriptionExpanded: Boolean,
        val resolutionDate: TextReference,
        val marketOpenedDate: TextReference,
        val resolutionSource: PolymarketResolutionSourceUM?,
    ) : PolymarketSoccerUM
}