package com.tangem.feature.referral.api.deeplink

interface ReferralDeepLinkHandler {

    interface Factory {
        fun create(): ReferralDeepLinkHandler
    }
}