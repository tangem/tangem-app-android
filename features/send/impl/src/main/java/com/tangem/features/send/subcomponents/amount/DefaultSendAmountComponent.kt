package com.tangem.features.send.subcomponents.amount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponent
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.subcomponents.amount.model.SendAmountModel
import com.tangem.features.send.subcomponents.amount.ui.SendAmountContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSendAmountComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendAmountComponentParams.AmountParams,
) : SendAmountComponent, AppComponentContext by appComponentContext {

    private val model: SendAmountModel = getOrCreateModel(params = params)

    override fun updateState(amountUM: AmountState) = model.updateState(amountUM)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val isSendWithSwapAvailable by model.isSendWithSwapAvailable.collectAsStateWithLifecycle()

        SendAmountContent(
            amountState = state,
            clickIntents = model,
            isSendWithSwapAvailable = isSendWithSwapAvailable,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : SendAmountComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SendAmountComponentParams.AmountParams,
        ): DefaultSendAmountComponent
    }
}