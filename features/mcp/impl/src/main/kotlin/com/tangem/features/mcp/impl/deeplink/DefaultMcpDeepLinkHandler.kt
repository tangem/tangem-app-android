package com.tangem.features.mcp.impl.deeplink

import com.tangem.common.routing.deeplink.DeeplinkConst.USER_CODE_KEY
import com.tangem.domain.mcp.McpRequestService
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.features.mcp.api.deeplink.McpDeepLinkHandler
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Handles the `tangem://mcp.authorization?user_code=...` deep link — the same pairing entry point the QR scanner
 * feeds, so both land on [McpRequestService] and share the WalletConnect request queue.
 */
internal class DefaultMcpDeepLinkHandler @AssistedInject constructor(
    @Assisted queryParams: Map<String, String>,
    mcpRequestService: McpRequestService,
) : McpDeepLinkHandler {

    init {
        val userCode = queryParams[USER_CODE_KEY]
        if (userCode.isNullOrBlank()) {
            TangemLogger.e("MCP deeplink without a $USER_CODE_KEY parameter")
        } else {
            mcpRequestService.request(McpRequest.Pair(userCode = userCode))
        }
    }

    @AssistedFactory
    interface Factory : McpDeepLinkHandler.Factory {
        override fun create(queryParams: Map<String, String>): DefaultMcpDeepLinkHandler
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://mcp.authorization"
    }
}