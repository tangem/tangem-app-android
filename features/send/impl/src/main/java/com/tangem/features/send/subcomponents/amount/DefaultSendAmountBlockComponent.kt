package com.tangem.features.send.subcomponents.amount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.ui.AmountBlockV2
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.api.subcomponents.amount.SendAmountBlockComponent
import com.tangem.features.send.api.subcomponents.amount.SendAmountComponentParams
import com.tangem.features.send.subcomponents.amount.model.SendAmountModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class DefaultSendAmountBlockComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendAmountComponentParams.AmountBlockParams,
    @Assisted val onResult: (AmountState) -> Unit,
    @Assisted val onClick: () -> Unit,
) : SendAmountBlockComponent, AppComponentContext by appComponentContext {

    private val model: SendAmountModel = getOrCreateModel(params = params)

    init {
        model.uiState.onEach {
            onResult(it)
        }.launchIn(componentScope)
    }

    override fun updateState(amountUM: AmountState) = model.updateState(amountUM)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val isClickEnabled by params.blockClickEnableFlow.collectAsStateWithLifecycle()

        AmountBlockV2(
            amountState = state,
            isClickDisabled = !isClickEnabled,
            isEditingDisabled = params.predefinedValues is PredefinedValues.Content.Deeplink,
            onClick = onClick,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : SendAmountBlockComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SendAmountComponentParams.AmountBlockParams,
            onClick: () -> Unit,
            onResult: (AmountState) -> Unit,
        ): DefaultSendAmountBlockComponent
    }
}