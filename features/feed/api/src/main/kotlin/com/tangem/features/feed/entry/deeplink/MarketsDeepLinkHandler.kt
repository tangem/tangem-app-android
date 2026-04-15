package com.tangem.features.feed.entry.deeplink

interface MarketsDeepLinkHandler {

    interface Factory {
        fun create(params: Map<String, String>): MarketsDeepLinkHandler
    }
}