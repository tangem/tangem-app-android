package com.tangem.features.mcp.impl.pair

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.mcp.impl.pair.model.McpPairModel
import com.tangem.features.mcp.impl.pair.ui.McpAgentInfoContent

internal class McpAgentInfoComponent(
    appComponentContext: AppComponentContext,
    private val model: McpPairModel,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        McpAgentInfoContent(state = state, modifier = modifier)
    }
}