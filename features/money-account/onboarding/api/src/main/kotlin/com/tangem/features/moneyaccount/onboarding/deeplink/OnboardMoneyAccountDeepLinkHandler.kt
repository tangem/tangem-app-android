package com.tangem.features.moneyaccount.onboarding.deeplink

import android.net.Uri

interface OnboardMoneyAccountDeepLinkHandler {

    interface Factory {
        fun create(uri: Uri): OnboardMoneyAccountDeepLinkHandler
    }
}