package com.tangem.features.send.v2.subcomponents.amount

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.ui.AmountBlock
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponentParams.AmountBlockParams
import com.tangem.features.send.v2.subcomponents.amount.model.SendAmountModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class SendAmountBlockComponent(
    appComponentContext: AppComponentContext,
    private val params: AmountBlockParams,
    val onResult: (AmountState) -> Unit,
    val onClick: () -> Unit,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SendAmountModel = getOrCreateModel(params = params, router = router)

    init {
        model.uiState.onEach {
            onResult(it)
        }.launchIn(componentScope)
    }

    fun updateState(amountUM: AmountState) = model.updateState(amountUM)

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.uiState.collectAsStateWithLifecycle()
        val isClickEnabled = params.blockClickEnableFlow.collectAsStateWithLifecycle()
        val isEditingDisabled = params.blockEditDisabledFlow.collectAsStateWithLifecycle()

        AmountBlock(
            amountState = state.value,
            isClickDisabled = !isClickEnabled.value,
            isEditingDisabled = isEditingDisabled.value,
            onClick = onClick,
        )
    }
}