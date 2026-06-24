package com.tangem.features.virtualaccount.onboarding.deeplink

import android.net.Uri

interface OnboardVirtualAccountsDeepLinkHandler {

    interface Factory {
        fun create(uri: Uri): OnboardVirtualAccountsDeepLinkHandler
    }
}