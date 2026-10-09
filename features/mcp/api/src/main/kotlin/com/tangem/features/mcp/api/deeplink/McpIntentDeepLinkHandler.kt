package com.tangem.features.mcp.api.deeplink

interface McpIntentDeepLinkHandler {

    interface Factory {
        fun create(queryParams: Map<String, String>): McpIntentDeepLinkHandler
    }
}