package com.tangem.features.tangempay.deeplink

import android.net.Uri

interface TangemPayMainDeepLinkHandler {

    companion object {
        private const val TANGEM_SCHEME = "tangem"
        private const val HTTPS_SCHEME = "https"
        private const val WEB_HOST = "tangem.com"
        private const val SEGMENT = "pay-app-main"

        const val TANGEM_DEEPLINK_KEY = "$TANGEM_SCHEME://$SEGMENT"
        const val HTTPS_DEEPLINK_KEY = "$HTTPS_SCHEME://$WEB_HOST/$SEGMENT"

        /** Whether [uri] is a Tangem Pay main deep link: [TANGEM_DEEPLINK_KEY] or a page under [HTTPS_DEEPLINK_KEY]. */
        fun canHandle(uri: Uri): Boolean = when (uri.scheme) {
            TANGEM_SCHEME -> uri.host == SEGMENT
            HTTPS_SCHEME -> uri.host == WEB_HOST && uri.isMainPath()
            else -> false
        }

        /** Whole-segment path match: `/pay-app-main` and `/pay-app-main/x` match, `/pay-app-mainx` does not. */
        private fun Uri.isMainPath(): Boolean {
            val path = path.orEmpty()
            return path == "/$SEGMENT" || path.startsWith("/$SEGMENT/") || pathSegments.firstOrNull() == SEGMENT
        }
    }
}