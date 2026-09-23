package com.tangem.features.tangempay.withdraw.bank.choice

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.withdraw.bank.choice.model.BankWithdrawChoiceConfig
import com.tangem.features.tangempay.withdraw.bank.choice.model.TangemPayBankWithdrawChoiceModel
import com.tangem.features.tangempay.withdraw.bank.choice.ui.TangemPayBankWithdrawChoiceScreen
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import kotlinx.coroutines.flow.StateFlow

internal class TangemPayBankWithdrawChoiceComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayBankWithdrawChoiceModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = state.onBackClick)
        TangemPayBankWithdrawChoiceScreen(state = state, modifier = modifier)
    }

    data class Params(
        val draft: StateFlow<BankWithdrawDraft>,
        val step: TangemPayBankWithdrawInnerRoute,
        val config: BankWithdrawChoiceConfig,
        val onContinue: (selectedId: String) -> Unit,
        val onCloseFlow: () -> Unit,
    )
}