package com.tangem.features.onramp.deeplink

import java.net.URI

interface BuyDeepLinkHandler {

    companion object {
        private const val SCHEME = "tangem"
        private const val HOST = "buy"

        const val DEEPLINK_KEY = "$SCHEME://$HOST"

        /** Whether [uri] is a buy deep link: `tangem://buy`, scheme compared case-insensitively. */
        fun canHandle(uri: URI): Boolean = uri.scheme.equals(SCHEME, ignoreCase = true) && uri.host == HOST
    }
}