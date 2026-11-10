package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.CATEGORY_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NEWS_ID_KEY
import javax.inject.Inject

internal class NewsDeepLinkHandler @Inject constructor(
    private val appRouter: AppRouter,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val queryParams = request.queryParams
        val newsId = queryParams[NEWS_ID_KEY]?.toIntOrNull()
        if (newsId != null) {
            appRouter.push(AppRoute.NewsDetails(newsId = newsId))
            return
        }

        appRouter.push(AppRoute.News(categoryId = queryParams[CATEGORY_ID_KEY]?.toIntOrNull()))
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://news"
    }
}