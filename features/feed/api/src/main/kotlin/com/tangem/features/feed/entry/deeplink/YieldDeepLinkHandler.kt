package com.tangem.features.feed.entry.deeplink

import kotlinx.coroutines.CoroutineScope

interface YieldDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, queryParams: Map<String, String>): YieldDeepLinkHandler
    }
}