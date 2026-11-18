package com.tangem.features.tangempay.deeplink

import com.tangem.core.navigation.deeplink.DeepLinkHandler

interface TangemPayOnboardDeepLinkHandler : DeepLinkHandler {

    companion object {
        const val TANGEM_DEEPLINK_KEY = "tangem://onboard-visa"
        const val HTTPS_DEEPLINK_KEY = "https://tangem.com/pay-app"

        /** The Money account campaign link; it opens the same onboarding, as on iOS */
        const val MONEY_ACCOUNT_DEEPLINK_KEY = "tangem://onboard-virtual-account"
    }
}