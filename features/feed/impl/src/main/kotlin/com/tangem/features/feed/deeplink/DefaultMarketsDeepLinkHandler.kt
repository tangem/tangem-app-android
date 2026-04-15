package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.INTERVAL_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.ORDER_KEY
import com.tangem.domain.markets.PreselectedMarketsInterval
import com.tangem.domain.markets.PreselectedMarketsOrder
import com.tangem.features.feed.entry.deeplink.MarketsDeepLinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMarketsDeepLinkHandler @AssistedInject constructor(
    @Assisted private val queryParams: Map<String, String>,
    appRouter: AppRouter,
) : MarketsDeepLinkHandler {

    init {
        appRouter.push(
            AppRoute.Markets(
                preselectedOrder = PreselectedMarketsOrder.parse(queryParams[ORDER_KEY]),
                preselectedInterval = PreselectedMarketsInterval.parse(queryParams[INTERVAL_KEY]),
            ),
        )
    }

    @AssistedFactory
    interface Factory : MarketsDeepLinkHandler.Factory {
        override fun create(params: Map<String, String>): DefaultMarketsDeepLinkHandler
    }
}