package com.tangem.features.feed.entry.deeplink

interface EarnDeepLinkHandler {

    interface Factory {
        fun create(queryParams: Map<String, String>): EarnDeepLinkHandler
    }
}