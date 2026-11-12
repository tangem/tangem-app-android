package com.tangem.features.tokendetails.deeplink

import android.net.Uri

interface TokenDetailsDeepLinkHandler {

    companion object {
        private const val SCHEME = "tangem"
        private const val HOST = "token"

        const val DEEPLINK_KEY = "$SCHEME://$HOST"

        /** Whether [uri] is a token details deep link: `tangem://token`. */
        fun canHandle(uri: Uri): Boolean = uri.scheme == SCHEME && uri.host == HOST
    }
}