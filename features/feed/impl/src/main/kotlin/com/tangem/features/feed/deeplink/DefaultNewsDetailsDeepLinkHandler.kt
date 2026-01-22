package com.tangem.features.feed.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.features.feed.entry.deeplink.NewsDetailsDeepLinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class DefaultNewsDetailsDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val deeplinkUri: Uri,
    private val appRouter: AppRouter,
) : NewsDetailsDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        scope.launch {
            val articleId = extractArticleIdFromUri(deeplinkUri)
            if (articleId == null) {
                Timber.e(
                    """
                        Failed to extract article ID from deep link
                        |- Received URI: $deeplinkUri
                    """.trimIndent(),
                )
                return@launch
            }

            appRouter.push(
                AppRoute.NewsDetails(newsId = articleId),
            )
        }
    }

    /**
     * Parsing URI with format https://tangem.com/news/{category}/{id}-{slug} to get id
     */
    private fun extractArticleIdFromUri(uri: Uri): Int? {
        val path = uri.path ?: return null
        val pathSegments = path.split("/").filter { it.isNotBlank() }
        if (pathSegments.isEmpty() || pathSegments[0] != "news" || pathSegments.size < 2) {
            return null
        }
        val lastSegment = pathSegments.last()
        val idPart = lastSegment.substringBefore("-")
        return idPart.toIntOrNull()
    }

    @AssistedFactory
    interface Factory : NewsDetailsDeepLinkHandler.Factory {
        override fun create(coroutineScope: CoroutineScope, deeplinkUri: Uri): DefaultNewsDetailsDeepLinkHandler
    }
}