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
import com.tangem.utils.logging.TangemLogger

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
            val articleId = NewsDetailsDeepLinkHandler.extractArticleIdFromUri(deeplinkUri)
            if (articleId == null) {
                TangemLogger.e(
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

    @AssistedFactory
    interface Factory : NewsDetailsDeepLinkHandler.Factory {
        override fun create(coroutineScope: CoroutineScope, deeplinkUri: Uri): DefaultNewsDetailsDeepLinkHandler
    }
}