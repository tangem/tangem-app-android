package com.tangem.core.deeplink.global

import com.tangem.core.deeplink.DeepLink

class BuyCurrencyDeepLink(val onReceive: (txId: String) -> Unit) : DeepLink {

    override val uri: String = "tangem://redirect?action=dismissBrowser"

    override fun onReceive(params: Map<String, String>) {
        onReceive(
            params["txId"] ?: return,
        )
    }
}