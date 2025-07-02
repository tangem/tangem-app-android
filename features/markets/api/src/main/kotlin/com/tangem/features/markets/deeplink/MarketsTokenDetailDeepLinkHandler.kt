package com.tangem.features.markets.deeplink

import kotlinx.coroutines.CoroutineScope

interface MarketsTokenDetailDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, params: Map<String, String>): MarketsTokenDetailDeepLinkHandler
    }
}