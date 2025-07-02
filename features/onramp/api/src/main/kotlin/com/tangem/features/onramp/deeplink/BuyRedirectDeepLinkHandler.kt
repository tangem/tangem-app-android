package com.tangem.features.onramp.deeplink

import kotlinx.coroutines.CoroutineScope

interface BuyRedirectDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope): BuyRedirectDeepLinkHandler
    }
}