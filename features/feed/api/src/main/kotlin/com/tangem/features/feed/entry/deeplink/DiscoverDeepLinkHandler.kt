package com.tangem.features.feed.entry.deeplink

interface DiscoverDeepLinkHandler {

    interface Factory {
        fun create(queryParams: Map<String, String>): DiscoverDeepLinkHandler
    }
}