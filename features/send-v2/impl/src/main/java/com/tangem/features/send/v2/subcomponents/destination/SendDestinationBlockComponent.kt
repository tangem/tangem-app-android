package com.tangem.features.send.v2.subcomponents.destination

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.subcomponents.destination.model.SendDestinationModel
import com.tangem.features.send.v2.subcomponents.destination.ui.DestinationBlock
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class SendDestinationBlockComponent(
    appComponentContext: AppComponentContext,
    private val params: SendDestinationComponentParams.DestinationBlockParams,
    val onClick: () -> Unit,
    val onResult: (DestinationUM) -> Unit,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SendDestinationModel = getOrCreateModel(params = params)

    init {
        model.uiState.onEach {
            onResult(it)
        }.launchIn(componentScope)
    }

    fun updateState(destinationUM: DestinationUM) = model.updateState(destinationUM)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val isClickEnabled by params.blockClickEnableFlow.collectAsStateWithLifecycle()

        DestinationBlock(
            destinationUM = state,
            isClickDisabled = !isClickEnabled,
            isEditingDisabled = params.isPredefinedValues,
            onClick = onClick,
        )
    }
}