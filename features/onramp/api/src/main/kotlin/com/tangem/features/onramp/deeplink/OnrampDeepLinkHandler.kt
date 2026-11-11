package com.tangem.features.onramp.deeplink

import java.net.URI

interface OnrampDeepLinkHandler {

    companion object {
        private const val SCHEME = "tangem"
        private const val HOST = "onramp"

        const val DEEPLINK_KEY = "$SCHEME://$HOST"

        /** Whether [uri] is an onramp deep link: `tangem://onramp`. */
        fun canHandle(uri: URI): Boolean = uri.scheme == SCHEME && uri.host == HOST
    }
}