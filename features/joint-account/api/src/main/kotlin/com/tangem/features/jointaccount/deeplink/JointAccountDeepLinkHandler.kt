package com.tangem.features.jointaccount.deeplink

import kotlinx.coroutines.CoroutineScope

interface JointAccountDeepLinkHandler {

    interface Factory {
        fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
            isFromOnNewIntent: Boolean,
        ): JointAccountDeepLinkHandler
    }
}