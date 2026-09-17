package com.tangem.features.feed.entry.deeplink

interface ShtorkaDeepLinkHandler {

    interface Factory {
        fun create(queryParams: Map<String, String>): ShtorkaDeepLinkHandler
    }
}