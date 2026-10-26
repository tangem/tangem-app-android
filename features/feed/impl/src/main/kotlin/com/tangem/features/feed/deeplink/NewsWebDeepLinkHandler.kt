package com.tangem.features.feed.deeplink

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.features.feed.entry.deeplink.NewsDeepLinkHandler
import com.tangem.features.feed.entry.deeplink.NewsDetailsDeepLinkHandler
import javax.inject.Inject

/**
 * Opens `https://tangem.com/news…`: the article when the link carries its id, otherwise the news list.
 * The browser fallback can't open article-less links either — the app claims `/news*` as a verified App Link.
 */
internal class NewsWebDeepLinkHandler @Inject constructor(
    private val newsDeepLinkHandlerFactory: NewsDeepLinkHandler.Factory,
    private val newsDetailsDeepLinkHandlerFactory: NewsDetailsDeepLinkHandler.Factory,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        if (NewsDetailsDeepLinkHandler.extractArticleIdFromUri(request.uri) != null) {
            newsDetailsDeepLinkHandlerFactory.create(coroutineScope = request.scope, deeplinkUri = request.uri)
        } else {
            newsDeepLinkHandlerFactory.create(queryParams = request.queryParams)
        }
    }

    companion object {
        const val DEEPLINK_KEY = "https://tangem.com/news"
    }
}