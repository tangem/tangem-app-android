package com.tangem.features.markets.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMarketsDeepLinkHandler @AssistedInject constructor(
    appRouter: AppRouter,
) : MarketsDeepLinkHandler {

    init {
        appRouter.push(AppRoute.Markets)
    }

    @AssistedFactory
    interface Factory : MarketsDeepLinkHandler.Factory {
        override fun create(): DefaultMarketsDeepLinkHandler
    }
}