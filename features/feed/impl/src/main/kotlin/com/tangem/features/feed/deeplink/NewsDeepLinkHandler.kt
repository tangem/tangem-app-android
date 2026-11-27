package com.tangem.features.feed.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeeplinkConst.CATEGORY_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NEWS_ID_KEY
import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import javax.inject.Inject

internal class NewsDeepLinkHandler @Inject constructor() : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val router = request.router
        val queryParams = request.queryParams
        val newsId = queryParams[NEWS_ID_KEY]?.toIntOrNull()
        if (newsId != null) {
            router.push(AppRoute.NewsDetails(newsId = newsId))
            return
        }

        router.push(AppRoute.News(categoryId = queryParams[CATEGORY_ID_KEY]?.toIntOrNull()))
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://news"
    }
}