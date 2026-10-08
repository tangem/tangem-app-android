package com.tangem.features.mcp.impl.deeplink

import com.tangem.common.routing.deeplink.DeeplinkConst.INTENT_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.SOURCE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.domain.mcp.McpRequestService
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.features.mcp.api.deeplink.McpIntentDeepLinkHandler
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Handles the `tangem://intent?id=...&source=...&type=...` deep link.
 *
 * Only Express exchange intents are acted on; an intent of any other service or operation is dropped, so it never
 * takes the place of the swap flow in the request queue. Express links may omit `type`, as the exchange is the only
 * operation it has had so far.
 */
internal class DefaultMcpIntentDeepLinkHandler @AssistedInject constructor(
    @Assisted queryParams: Map<String, String>,
    mcpRequestService: McpRequestService,
) : McpIntentDeepLinkHandler {

    init {
        val id = queryParams[INTENT_ID_KEY]?.takeIf { it.isNotBlank() }
        val source = queryParams[SOURCE_KEY]?.takeIf { it.isNotBlank() }
        val type = queryParams[TYPE_KEY]?.takeIf { it.isNotBlank() }

        if (id == null || source == null) {
            TangemLogger.e("MCP intent deeplink requires $INTENT_ID_KEY and $SOURCE_KEY parameters")
        } else {
            val intentSource = parseSource(source = source, type = type)
            if (intentSource == SUPPORTED_SOURCE) {
                mcpRequestService.request(McpRequest.Intent(id = id, source = intentSource))
            } else {
                TangemLogger.e("MCP intent of an unsupported kind: $SOURCE_KEY=$source, $TYPE_KEY=$type")
            }
        }
    }

    private fun parseSource(source: String, type: String?): McpRequest.Intent.Source? = when (source) {
        SOURCE_EXPRESS -> {
            val expressType = if (type == null) {
                McpRequest.Intent.Source.Express.Type.EXCHANGE
            } else {
                McpRequest.Intent.Source.Express.Type.entries.firstOrNull { it.name.lowercase() == type }
            }
            expressType?.let(McpRequest.Intent.Source::Express)
        }
        else -> null
    }

    @AssistedFactory
    interface Factory : McpIntentDeepLinkHandler.Factory {
        override fun create(queryParams: Map<String, String>): DefaultMcpIntentDeepLinkHandler
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://intent"

        private const val SOURCE_EXPRESS = "express"

        private val SUPPORTED_SOURCE = McpRequest.Intent.Source.Express(McpRequest.Intent.Source.Express.Type.EXCHANGE)
    }
}