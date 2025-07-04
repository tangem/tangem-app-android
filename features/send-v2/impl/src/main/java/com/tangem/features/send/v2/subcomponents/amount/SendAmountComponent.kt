package com.tangem.features.send.v2.subcomponents.amount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationModelCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponentParams.AmountParams
import com.tangem.features.send.v2.subcomponents.amount.model.SendAmountModel
import com.tangem.features.send.v2.subcomponents.amount.ui.SendAmountContent

internal class SendAmountComponent(
    appComponentContext: AppComponentContext,
    private val params: AmountParams,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SendAmountModel = getOrCreateModel(params = params)

    fun updateState(amountUM: AmountState) = model.updateState(amountUM)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val isBalanceHidden by params.isBalanceHidingFlow.collectAsStateWithLifecycle()

        SendAmountContent(
            amountState = state,
            isBalanceHidden = isBalanceHidden,
            clickIntents = model,
            isSendWithSwapEnabled = model.isSendWithSwapEnabled,
            modifier = modifier,
        )
    }

    interface ModelCallback : NavigationModelCallback {
        fun onAmountResult(amountUM: AmountState, isResetPredefined: Boolean)
        fun onConvertToAnotherToken(lastAmount: String)
    }
}