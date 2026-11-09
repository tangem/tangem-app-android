package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.INTERVAL_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.ORDER_KEY
import com.tangem.domain.markets.PreselectedMarketsInterval
import com.tangem.domain.markets.PreselectedMarketsOrder
import javax.inject.Inject

internal class MarketsDeepLinkHandler @Inject constructor(
    private val appRouter: AppRouter,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val queryParams = request.queryParams
        appRouter.push(
            AppRoute.Markets(
                preselectedOrder = PreselectedMarketsOrder.parse(queryParams[ORDER_KEY]),
                preselectedInterval = PreselectedMarketsInterval.parse(queryParams[INTERVAL_KEY]),
            ),
        )
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://markets"
    }
}