package com.tangem.features.tangempay.deeplink

import kotlinx.coroutines.CoroutineScope

interface TangemPayMainDeepLinkHandler {

    interface Factory {
        fun create(scope: CoroutineScope, payload: Map<String, String>): TangemPayMainDeepLinkHandler
    }
}