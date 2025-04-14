package com.tangem.features.send.v2.subcomponents.destination

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.common.SendNavigationModelCallback
import com.tangem.features.send.v2.subcomponents.destination.model.SendDestinationModel
import com.tangem.features.send.v2.subcomponents.destination.ui.SendDestinationContent
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM

internal class SendDestinationComponent(
    appComponentContext: AppComponentContext,
    private val params: SendDestinationComponentParams.DestinationParams,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SendDestinationModel = getOrCreateModel(params = params)

    fun updateState(state: DestinationUM) = model.updateState(state)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val isBalanceHidden by params.isBalanceHidingFlow.collectAsStateWithLifecycle()

        SendDestinationContent(state = state, clickIntents = model, isBalanceHidden = isBalanceHidden)
    }

    interface ModelCallback : SendNavigationModelCallback {
        fun onDestinationResult(destinationUM: DestinationUM)
    }
}