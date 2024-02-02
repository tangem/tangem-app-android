package com.tangem.core.deeplink.global

import com.tangem.core.deeplink.DeepLink

class BuyCurrencyDeepLink(val onReceive: () -> Unit) : DeepLink {

    override val uri: String = "tangem://success.tangem.com"

    override fun onReceive(params: Map<String, String>) {
        onReceive()
    }
}