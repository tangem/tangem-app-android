package com.tangem.features.send.v2.api.deeplink

import kotlinx.coroutines.CoroutineScope

interface SellRedirectDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, queryParams: Map<String, String>): SellRedirectDeepLinkHandler
    }
}