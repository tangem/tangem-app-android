package com.tangem.features.staking.api.deeplink

import kotlinx.coroutines.CoroutineScope

interface StakingDeepLinkHandler {

    interface Factory {
        fun create(coroutineScope: CoroutineScope, queryParams: Map<String, String>): StakingDeepLinkHandler
    }
}