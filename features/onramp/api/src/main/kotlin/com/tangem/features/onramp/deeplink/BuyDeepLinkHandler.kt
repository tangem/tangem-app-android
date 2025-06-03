package com.tangem.features.onramp.deeplink

import kotlinx.coroutines.CoroutineScope

interface BuyDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope): BuyDeepLinkHandler
    }
}