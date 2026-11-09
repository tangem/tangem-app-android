package com.tangem.features.tangempay.deeplink

import android.net.Uri

interface TangemPayOnboardDeepLinkHandler {

    interface Factory {
        fun create(uri: Uri): TangemPayOnboardDeepLinkHandler
    }
}