package com.tangem.features.mcp.impl

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.features.mcp.api.McpComponentFactory
import com.tangem.features.mcp.impl.pair.McpPairComponent
import javax.inject.Inject

internal class DefaultMcpComponentFactory @Inject constructor(
    private val mcpPairComponentFactory: McpPairComponent.Factory,
) : McpComponentFactory {

    override fun create(context: AppComponentContext, request: McpRequest): ComposableContentComponent {
        return when (request) {
            is McpRequest.Pair -> mcpPairComponentFactory
                .create(context = context, params = McpPairComponent.Params(request))
        }
    }
}