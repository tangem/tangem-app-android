package com.tangem.features.feed.entry.deeplink

import kotlinx.coroutines.CoroutineScope

interface MarketsTokenExchangesDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, params: Map<String, String>): MarketsTokenExchangesDeepLinkHandler
    }
}