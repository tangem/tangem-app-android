package com.tangem.features.tangempay.withdraw.bank.preview

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.preview.model.TangemPayBankWithdrawPreviewModel
import com.tangem.features.tangempay.withdraw.bank.preview.ui.TangemPayBankWithdrawPreviewScreen
import kotlinx.coroutines.flow.StateFlow

internal class TangemPayBankWithdrawPreviewComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayBankWithdrawPreviewModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler(onBack = state.onBackClick)
        TangemPayBankWithdrawPreviewScreen(state = state, modifier = modifier)
    }

    data class Params(
        val userWalletId: UserWalletId,
        val draft: StateFlow<BankWithdrawDraft>,
        val onOrderCreated: (BankOfframpOrderResult) -> Unit,
        val onOrderSigned: () -> Unit,
    )
}