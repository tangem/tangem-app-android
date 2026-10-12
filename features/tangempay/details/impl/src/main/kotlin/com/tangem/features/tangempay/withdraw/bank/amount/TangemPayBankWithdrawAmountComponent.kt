package com.tangem.features.tangempay.withdraw.bank.amount

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.withdraw.bank.amount.model.TangemPayBankWithdrawAmountModel
import com.tangem.features.tangempay.withdraw.bank.amount.ui.TangemPayBankWithdrawAmountScreen
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawAmountResult
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import kotlinx.coroutines.flow.StateFlow

internal class TangemPayBankWithdrawAmountComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayBankWithdrawAmountModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = state.onBackClick)
        TangemPayBankWithdrawAmountScreen(state = state, modifier = modifier)
    }

    data class Params(
        val draft: StateFlow<BankWithdrawDraft>,
        val options: StateFlow<BankWithdrawOptions>,
        val step: TangemPayBankWithdrawInnerRoute,
        val onContinue: (BankWithdrawAmountResult) -> Unit,
        val onCloseFlow: () -> Unit,
    )
}