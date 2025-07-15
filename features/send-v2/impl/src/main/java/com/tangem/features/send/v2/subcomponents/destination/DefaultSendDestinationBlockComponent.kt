package com.tangem.features.send.v2.subcomponents.destination

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.send.v2.api.entity.PredefinedValues
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationBlockComponent
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.subcomponents.destination.model.SendDestinationModel
import com.tangem.features.send.v2.subcomponents.destination.ui.DestinationBlock
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class DefaultSendDestinationBlockComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendDestinationComponentParams.DestinationBlockParams,
    @Assisted val onClick: () -> Unit,
    @Assisted val onResult: (DestinationUM) -> Unit,
) : SendDestinationBlockComponent, AppComponentContext by appComponentContext {

    private val model: SendDestinationModel = getOrCreateModel(params = params)

    init {
        model.uiState.onEach {
            onResult(it)
        }.launchIn(componentScope)
    }

    override fun updateState(destinationUM: DestinationUM) = model.updateState(destinationUM)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val isClickEnabled by params.blockClickEnableFlow.collectAsStateWithLifecycle()

        DestinationBlock(
            destinationUM = state,
            isClickDisabled = !isClickEnabled,
            isEditingDisabled = params.predefinedValues is PredefinedValues.Content.Deeplink,
            isRedesignEnabled = (params.state as? DestinationUM.Content)?.isRedesignEnabled ?: false,
            onClick = onClick,
        )
    }

    @AssistedFactory
    interface Factory : SendDestinationBlockComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SendDestinationComponentParams.DestinationBlockParams,
            onClick: () -> Unit,
            onResult: (DestinationUM) -> Unit,
        ): DefaultSendDestinationBlockComponent
    }
}