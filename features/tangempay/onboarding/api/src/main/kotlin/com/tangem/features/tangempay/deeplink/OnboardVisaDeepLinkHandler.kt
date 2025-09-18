package com.tangem.features.tangempay.deeplink

import android.net.Uri

interface OnboardVisaDeepLinkHandler {

    interface Factory {
        fun create(uri: Uri): OnboardVisaDeepLinkHandler
    }
}