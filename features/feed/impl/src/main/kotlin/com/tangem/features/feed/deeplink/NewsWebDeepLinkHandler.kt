package com.tangem.features.feed.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Opens `https://tangem.com/news…`: the article when the link carries its id, otherwise the news list.
 * The browser fallback can't open article-less links either — the app claims `/news*` as a verified App Link.
 */
internal class NewsWebDeepLinkHandler @Inject constructor(
    private val newsDeepLinkHandler: NewsDeepLinkHandler,
    private val appRouter: AppRouter,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val articleId = extractArticleIdFromUri(request.uri)
        if (articleId != null) {
            request.scope.launch {
                appRouter.push(AppRoute.NewsDetails(newsId = articleId))
            }
        } else {
            newsDeepLinkHandler.handle(request)
        }
    }

    /**
     * Parses a URI of the form `https://tangem.com/news/{category}/{id}-{slug}` into the article id.
     * Returns `null` when the path carries no article id — e.g. the bare `/news` listing page or
     * a `/news/{category}` page.
     */
    private fun extractArticleIdFromUri(uri: Uri): Int? {
        val pathSegments = uri.path.orEmpty().split("/").filter { it.isNotBlank() }
        if (pathSegments.isEmpty() || pathSegments[0] != "news" || pathSegments.size < 2) {
            return null
        }
        return pathSegments.last().substringBefore("-").toIntOrNull()
    }

    companion object {
        const val DEEPLINK_KEY = "https://tangem.com/news"
    }
}