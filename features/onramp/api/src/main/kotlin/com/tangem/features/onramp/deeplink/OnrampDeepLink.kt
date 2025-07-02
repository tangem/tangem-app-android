package com.tangem.features.onramp.deeplink

import com.tangem.core.deeplink.DeepLink
import kotlinx.coroutines.CoroutineScope

@Deprecated("Use OnrampDeepLinkHandler")
abstract class OnrampDeepLink : DeepLink() {
    override val uri = "tangem://onramp"

    interface Factory {
        fun create(coroutineScope: CoroutineScope): OnrampDeepLink
    }
}

interface OnrampDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, queryParams: Map<String, String>): OnrampDeepLinkHandler
    }
}