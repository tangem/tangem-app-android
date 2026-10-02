package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.connections.model.McpSessionInfoModel
import com.tangem.features.walletconnect.connections.ui.McpSessionInfoBS

internal class McpSessionInfoComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    private val model: McpSessionInfoModel = getOrCreateModel(params)

    override fun dismiss() {
        model.uiState.value.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()

        McpSessionInfoBS(state = state)
    }

    data class Params(val sessionId: String, val onDismiss: () -> Unit)
}