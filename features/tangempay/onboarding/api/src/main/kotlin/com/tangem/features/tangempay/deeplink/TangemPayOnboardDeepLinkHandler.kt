package com.tangem.features.tangempay.deeplink

import com.tangem.common.routing.deeplink.DeepLinkHandler

interface TangemPayOnboardDeepLinkHandler : DeepLinkHandler {

    companion object {
        const val TANGEM_DEEPLINK_KEY = "tangem://onboard-visa"
        const val HTTPS_DEEPLINK_KEY = "https://tangem.com/pay-app"
    }
}