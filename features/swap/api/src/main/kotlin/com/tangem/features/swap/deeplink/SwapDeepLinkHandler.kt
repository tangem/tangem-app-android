package com.tangem.features.swap.deeplink

import kotlinx.coroutines.CoroutineScope

interface SwapDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, queryParams: Map<String, String>): SwapDeepLinkHandler
    }
}