package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.walletconnect.connections.model.WcConnectionsModel
import com.tangem.features.walletconnect.connections.ui.WcConnectionsContent

internal class ConnectionsComponent(
    appComponentContext: AppComponentContext,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcConnectionsModel = getOrCreateModel()

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        WcConnectionsContent(modifier = modifier, state = state)
    }
}