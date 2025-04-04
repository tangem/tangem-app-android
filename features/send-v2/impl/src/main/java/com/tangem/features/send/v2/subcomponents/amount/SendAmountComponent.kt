package com.tangem.features.send.v2.subcomponents.amount

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.amountScreen.AmountScreenContent
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.common.SendNavigationModelCallback
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponentParams.AmountParams
import com.tangem.features.send.v2.subcomponents.amount.model.SendAmountModel

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

        AmountScreenContent(
            amountState = state,
            isBalanceHidden = isBalanceHidden,
            clickIntents = model,
            modifier = Modifier.background(TangemTheme.colors.background.tertiary),
        )
    }

    interface ModelCallback : SendNavigationModelCallback {
        fun onAmountResult(amountUM: AmountState)
    }
}