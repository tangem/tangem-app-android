package com.tangem.features.tangempay.withdraw.bank.form

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.model.TangemPayBankWithdrawFormModel
import com.tangem.features.tangempay.withdraw.bank.form.ui.TangemPayBankWithdrawFormScreen
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

internal class TangemPayBankWithdrawFormComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayBankWithdrawFormModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = state.onBackClick)
        TangemPayBankWithdrawFormScreen(state = state, modifier = modifier)
    }

    data class Params(
        val draft: StateFlow<BankWithdrawDraft>,
        val step: TangemPayBankWithdrawInnerRoute,
        val fields: ImmutableList<BankWithdrawFormField>,
        val onBack: (values: Map<BankWithdrawFormField, String>) -> Unit,
        val onContinue: (values: Map<BankWithdrawFormField, String>) -> Unit,
    )
}