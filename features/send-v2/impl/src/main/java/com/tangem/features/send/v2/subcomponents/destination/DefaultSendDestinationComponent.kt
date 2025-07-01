package com.tangem.features.send.v2.subcomponents.destination

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.subcomponents.destination.model.SendDestinationModel
import com.tangem.features.send.v2.subcomponents.destination.ui.SendDestinationContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSendDestinationComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendDestinationComponentParams.DestinationParams,
) : SendDestinationComponent, AppComponentContext by appComponentContext {

    private val model: SendDestinationModel = getOrCreateModel(params = params)

    fun updateState(state: DestinationUM) = model.updateState(state)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val isBalanceHidden by params.isBalanceHidingFlow.collectAsStateWithLifecycle()

        SendDestinationContent(state = state, clickIntents = model, isBalanceHidden = isBalanceHidden)
    }

    @AssistedFactory
    interface Factory : SendDestinationComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SendDestinationComponentParams.DestinationParams,
        ): DefaultSendDestinationComponent
    }
}