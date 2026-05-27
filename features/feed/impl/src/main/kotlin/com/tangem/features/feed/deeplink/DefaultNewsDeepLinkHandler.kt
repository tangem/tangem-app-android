package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.CATEGORY_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NEWS_ID_KEY
import com.tangem.features.feed.entry.deeplink.NewsDeepLinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNewsDeepLinkHandler @AssistedInject constructor(
    @Assisted private val queryParams: Map<String, String>,
    private val appRouter: AppRouter,
) : NewsDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val newsId = queryParams[NEWS_ID_KEY]?.toIntOrNull()
        if (newsId != null) {
            appRouter.push(AppRoute.NewsDetails(newsId = newsId))
            return
        }

        appRouter.push(AppRoute.News(categoryId = queryParams[CATEGORY_ID_KEY]?.toIntOrNull()))
    }

    @AssistedFactory
    interface Factory : NewsDeepLinkHandler.Factory {
        override fun create(queryParams: Map<String, String>): DefaultNewsDeepLinkHandler
    }
}