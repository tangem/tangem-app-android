package com.tangem.core.deeplink.global

import com.tangem.core.deeplink.DeepLink

class BuyCurrencyDeepLink(
    val onReceive: (externalTxId: String) -> Unit,
) : DeepLink() {

    override val uri = BUY_REDIRECT_DEEPLINK

    override fun onReceive(params: Map<String, String>) {
        onReceive(
            params["merchant_transaction_id"] ?: return,
        )
    }

    companion object {
        const val ONRAMP_REDIRECT_DEEPLINK = "https://tangem.com/success?action=dismissBrowser"
        private const val BUY_REDIRECT_DEEPLINK = "tangem://redirect?action=dismissBrowser"
    }
}