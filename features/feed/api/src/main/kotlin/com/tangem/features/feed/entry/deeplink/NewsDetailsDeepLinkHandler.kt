package com.tangem.features.feed.entry.deeplink

import android.net.Uri
import kotlinx.coroutines.CoroutineScope

interface NewsDetailsDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, deeplinkUri: Uri): NewsDetailsDeepLinkHandler
    }

    companion object {

        /**
         * Parses a URI of the form `https://tangem.com/news/{category}/{id}-{slug}` into the article id.
         * Returns `null` when the path carries no article id — e.g. the bare `/news` listing page or
         * a `/news/{category}` page — so callers can decide the route is not claimable before dispatching.
         */
        fun extractArticleIdFromUri(uri: Uri): Int? {
            val pathSegments = uri.path.orEmpty().split("/").filter { it.isNotBlank() }
            if (pathSegments.isEmpty() || pathSegments[0] != "news" || pathSegments.size < 2) {
                return null
            }
            return pathSegments.last().substringBefore("-").toIntOrNull()
        }
    }
}