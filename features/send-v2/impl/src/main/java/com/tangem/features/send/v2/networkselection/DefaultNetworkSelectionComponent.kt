package com.tangem.features.send.v2.networkselection

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.send.v2.api.NetworkSelectionComponent
import com.tangem.features.send.v2.networkselection.model.NetworkSelectionModel
import com.tangem.features.send.v2.networkselection.ui.NetworkSelectionScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultNetworkSelectionComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: NetworkSelectionComponent.Params,
) : NetworkSelectionComponent, AppComponentContext by context {

    private val model: NetworkSelectionModel = getOrCreateModel(params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun Dialog() {
        val state = model.uiState.collectAsStateWithLifecycle()
        NetworkSelectionScreen(state = state.value, onDismiss = ::dismiss)
    }

    @AssistedFactory
    interface Factory : NetworkSelectionComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NetworkSelectionComponent.Params,
        ): DefaultNetworkSelectionComponent
    }
}