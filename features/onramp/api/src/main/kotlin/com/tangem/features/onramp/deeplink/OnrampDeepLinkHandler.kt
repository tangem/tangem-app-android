package com.tangem.features.onramp.deeplink

import kotlinx.coroutines.CoroutineScope

interface OnrampDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, queryParams: Map<String, String>): OnrampDeepLinkHandler
    }
}