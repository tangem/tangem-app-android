package com.tangem.core.deeplink.global

import com.tangem.core.deeplink.DeepLink

@Deprecated("Use BuyDeepLinkHandler")
class BuyCurrencyDeepLink(
    val onReceive: () -> Unit,
) : DeepLink() {

    override val uri = BUY_REDIRECT_DEEPLINK

    override fun onReceive(params: Map<String, String>) {
        onReceive()
    }

    companion object {
        private const val BUY_REDIRECT_DEEPLINK = "tangem://redirect?action=dismissBrowser"
    }
}