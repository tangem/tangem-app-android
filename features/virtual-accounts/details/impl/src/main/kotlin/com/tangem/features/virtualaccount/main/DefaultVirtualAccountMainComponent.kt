package com.tangem.features.virtualaccount.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.virtualaccount.details.component.VirtualAccountMainComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultVirtualAccountMainComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: VirtualAccountMainComponent.Params,
) : VirtualAccountMainComponent, AppComponentContext by appComponentContext {

    private val model: VirtualAccountMainModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        VirtualAccountMainScreen(state = state, modifier = modifier)
    }

    @AssistedFactory
    interface Factory : VirtualAccountMainComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: VirtualAccountMainComponent.Params,
        ): DefaultVirtualAccountMainComponent
    }
}