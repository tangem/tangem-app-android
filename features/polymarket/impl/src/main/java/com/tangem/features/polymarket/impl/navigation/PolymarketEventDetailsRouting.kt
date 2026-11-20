package com.tangem.features.polymarket.impl.navigation

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent
import com.tangem.features.polymarket.impl.soccer.PolymarketSoccerComponent

private const val SOCCER_EVENT_TYPE = "soccer"
private const val MATCH_TEAMS_COUNT = 2

internal val PolymarketEvent.isSoccerMatch: Boolean
    get() = eventType == SOCCER_EVENT_TYPE && teams?.size == MATCH_TEAMS_COUNT

internal fun PolymarketRoute.EventDetails.createEventDetailsComponent(
    context: AppComponentContext,
    isPhaseTwoEnabled: Boolean,
): ComposableContentComponent = if (isPhaseTwoEnabled && isSoccerMatch == true) {
    PolymarketSoccerComponent(context, PolymarketSoccerComponent.Params(eventId, userWalletId))
} else {
    PolymarketEventDetailsComponent(
        context,
        PolymarketEventDetailsComponent.Params(
            eventId = eventId,
            userWalletId = userWalletId,
            marketId = marketId,
            assetId = assetId,
            categoryLabel = categoryLabel,
            isSoccerMatch = isSoccerMatch,
        ),
    )
}