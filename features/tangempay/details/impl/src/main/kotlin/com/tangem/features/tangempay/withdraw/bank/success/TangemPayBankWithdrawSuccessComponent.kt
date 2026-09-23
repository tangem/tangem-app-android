package com.tangem.features.tangempay.withdraw.bank.success

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.success.state.TangemPayBankWithdrawSuccessUMFactory
import com.tangem.features.tangempay.withdraw.bank.success.ui.TangemPayBankWithdrawSuccessScreen
import kotlinx.coroutines.flow.StateFlow

/** Closing state of the wizard: the order is signed and the backend is moving the money. */
internal class TangemPayBankWithdrawSuccessComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    // The signed order carries no date, so the transfer is dated by the moment this closing step is reached; the
    // instance keeper holds it so an Activity recreation does not move the date to "now".
    private val shownAt: Long = instanceKeeper.getOrCreateSimple(key = SHOWN_AT_KEY) { System.currentTimeMillis() }

    @Composable
    override fun Content(modifier: Modifier) {
        val draft by params.draft.collectAsStateWithLifecycle()
        BackHandler(onBack = params.onDoneClick)
        val state = remember(draft) {
            TangemPayBankWithdrawSuccessUMFactory.create(
                draft = draft,
                shownAt = shownAt,
                onCloseClick = params.onDoneClick,
            )
        }
        TangemPayBankWithdrawSuccessScreen(state = state, modifier = modifier)
    }

    data class Params(
        val draft: StateFlow<BankWithdrawDraft>,
        val onDoneClick: () -> Unit,
    )

    private companion object {
        const val SHOWN_AT_KEY = "shownAt"
    }
}