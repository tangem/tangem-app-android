package com.tangem.features.feed.entry.deeplink

interface MarketsDeepLinkHandler {

    interface Factory {
        fun create(): MarketsDeepLinkHandler
    }
}