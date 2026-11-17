package com.tangem.features.polymarket.impl.navigation

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent
import com.tangem.features.polymarket.impl.soccer.PolymarketSoccerComponent

internal const val SOCCER_EVENT_TYPE = "soccer"

internal fun PolymarketRoute.EventDetails.createEventDetailsComponent(
    context: AppComponentContext,
    isPhaseTwoEnabled: Boolean,
): ComposableContentComponent = if (isPhaseTwoEnabled && eventType == SOCCER_EVENT_TYPE) {
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
        ),
    )
}