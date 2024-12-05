package com.tangem.core.deeplink.global

import com.tangem.core.deeplink.DeepLink

class BuyCurrencyDeepLink(
    isOnrampFeatureEnabled: Boolean,
    val onReceive: (externalTxId: String) -> Unit,
) : DeepLink {

    override val uri: String = if (isOnrampFeatureEnabled) {
        ONRAMP_REDIRECT_DEEPLINK
    } else {
        BUY_REDIRECT_DEEPLINK
    }

    override fun onReceive(params: Map<String, String>) {
        onReceive(
            params["merchant_transaction_id"] ?: return,
        )
    }

    companion object {
        const val ONRAMP_REDIRECT_DEEPLINK = "tangem://onramp-success?"
        private const val BUY_REDIRECT_DEEPLINK = "tangem://redirect?action=dismissBrowser"
    }
}