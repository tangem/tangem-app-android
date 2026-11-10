package com.tangem.features.mcp.api.deeplink

import android.net.Uri

interface McpDeepLinkHandler {

    companion object {
        private const val SCHEME = "tangem"
        private const val HOST = "mcp.authorization"

        const val DEEPLINK_KEY = "$SCHEME://$HOST"

        /** Whether [uri] is an MCP pairing deep link: `tangem://mcp.authorization`. */
        fun canHandle(uri: Uri): Boolean = uri.scheme == SCHEME && uri.host == HOST
    }
}