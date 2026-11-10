package com.tangem.features.mcp.impl.deeplink

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.USER_CODE_KEY
import com.tangem.domain.mcp.McpRequestService
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.features.mcp.api.McpFeatureToggles
import com.tangem.features.mcp.api.deeplink.McpDeepLinkHandler
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Handles the `tangem://mcp.authorization?user_code=...` deep link — the same pairing entry point the QR scanner
 * feeds, so both land on [McpRequestService] and share the WalletConnect request queue.
 */
internal class DefaultMcpDeepLinkHandler @Inject constructor(
    private val mcpRequestService: McpRequestService,
    private val mcpFeatureToggles: McpFeatureToggles,
) : DeepLinkHandler, McpDeepLinkHandler {

    override val isEnabled: Boolean
        get() = mcpFeatureToggles.isMcpEnabled

    /** The link pairs an agent with a payload-supplied user code. */
    override val isOpenableFromPush: Boolean = false

    override fun handle(request: DeepLinkRequest) {
        val userCode = request.queryParams[USER_CODE_KEY]
        if (userCode.isNullOrBlank()) {
            TangemLogger.e("MCP deeplink without a $USER_CODE_KEY parameter")
        } else {
            mcpRequestService.request(McpRequest.Pair(userCode = userCode))
        }
    }
}