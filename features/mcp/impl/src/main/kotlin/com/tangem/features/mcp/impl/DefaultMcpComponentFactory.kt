package com.tangem.features.mcp.impl

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.features.mcp.api.McpComponentFactory
import com.tangem.features.mcp.impl.pair.McpPairComponent
import com.tangem.features.swap.mcp.ExchangeIntentComponent
import javax.inject.Inject

internal class DefaultMcpComponentFactory @Inject constructor(
    private val mcpPairComponentFactory: McpPairComponent.Factory,
    private val exchangeIntentComponentFactory: ExchangeIntentComponent.Factory,
) : McpComponentFactory {

    override fun create(context: AppComponentContext, request: McpRequest): ComposableContentComponent {
        return when (request) {
            is McpRequest.Pair -> mcpPairComponentFactory
                .create(context = context, params = McpPairComponent.Params(request))
            is McpRequest.Intent -> createIntentComponent(context = context, request = request)
        }
    }

    private fun createIntentComponent(context: AppComponentContext, request: McpRequest.Intent) =
        when (val source = request.source) {
            is McpRequest.Intent.Source.Express -> when (source.type) {
                McpRequest.Intent.Source.Express.Type.EXCHANGE -> exchangeIntentComponentFactory
                    .create(context = context, params = ExchangeIntentComponent.Params(intentId = request.id))
            }
        }
}