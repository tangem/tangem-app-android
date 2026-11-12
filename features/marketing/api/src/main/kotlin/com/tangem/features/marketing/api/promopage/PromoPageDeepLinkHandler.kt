package com.tangem.features.marketing.api.promopage

import android.net.Uri

interface PromoPageDeepLinkHandler {

    companion object {
        private const val SCHEME = "tangem"
        private const val HOST = "promo_page"

        const val DEEPLINK_KEY = "$SCHEME://$HOST"

        /** Whether [uri] is a promo page deep link: `tangem://promo_page`. */
        fun canHandle(uri: Uri): Boolean = uri.scheme == SCHEME && uri.host == HOST
    }
}