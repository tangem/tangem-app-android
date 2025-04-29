package com.tangem.features.onramp.deeplink

import com.tangem.core.deeplink.DeepLink
import kotlinx.coroutines.CoroutineScope

abstract class OnrampDeepLink : DeepLink() {
    override val uri = "tangem://onramp"

    interface Factory {
        fun create(coroutineScope: CoroutineScope): OnrampDeepLink
    }
}