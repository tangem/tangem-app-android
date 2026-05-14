package com.tangem.features.feed.entry.deeplink

interface NewsDeepLinkHandler {

    interface Factory {
        fun create(queryParams: Map<String, String>): NewsDeepLinkHandler
    }
}