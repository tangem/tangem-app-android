package com.tangem.core.deeplink.global

import com.tangem.core.deeplink.DeepLink

@Deprecated("Use ReferralDeepLinkHandler")
class ReferralDeepLink(
    val onReceive: () -> Unit,
) : DeepLink(shouldHandleDelayed = true) {
    override val uri: String = "tangem://referral"

    override fun onReceive(params: Map<String, String>) {
        onReceive()
    }
}