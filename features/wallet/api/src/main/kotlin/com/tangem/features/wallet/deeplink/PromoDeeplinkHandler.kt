package com.tangem.features.wallet.deeplink

import kotlinx.coroutines.CoroutineScope

interface PromoDeeplinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, queryParams: Map<String, String>): PromoDeeplinkHandler
    }
}