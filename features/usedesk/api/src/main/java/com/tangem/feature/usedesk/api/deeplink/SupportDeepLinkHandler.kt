package com.tangem.feature.usedesk.api.deeplink

import kotlinx.coroutines.CoroutineScope

interface SupportDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope): SupportDeepLinkHandler
    }
}