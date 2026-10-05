package com.tangem.features.mcp.api.deeplink

interface McpDeepLinkHandler {

    interface Factory {
        fun create(queryParams: Map<String, String>): McpDeepLinkHandler
    }
}