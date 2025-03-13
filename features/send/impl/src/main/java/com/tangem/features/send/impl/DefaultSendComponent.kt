package com.tangem.features.send.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.send.api.SendComponent
import com.tangem.features.send.impl.presentation.model.SendModel
import com.tangem.features.send.impl.presentation.ui.SendScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSendComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: SendComponent.Params,
) : SendComponent, AppComponentContext by appComponentContext {

    private val model: SendModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val currentState = model.stateRouter.currentState.collectAsStateWithLifecycle()
        val uiState by model.uiState.collectAsStateWithLifecycle()

        SendScreen(uiState, currentState.value)
    }

    @AssistedFactory
    interface Factory : SendComponent.Factory {
        override fun create(context: AppComponentContext, params: SendComponent.Params): DefaultSendComponent
    }
}