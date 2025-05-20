package com.tangem.features.tokendetails.deeplink

import kotlinx.coroutines.CoroutineScope

interface TokenDetailsDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, params: Map<String, String>): TokenDetailsDeepLinkHandler
    }
}