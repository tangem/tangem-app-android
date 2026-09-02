package com.tangem.features.tangempay.deeplink

import kotlinx.coroutines.CoroutineScope

interface TangemPayAccountDeepLinkHandler {

    interface Factory {
        fun create(scope: CoroutineScope, queryParams: Map<String, String>): TangemPayAccountDeepLinkHandler
    }
}