package com.tangem.features.tangempay.withdraw.bank.success

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.withdraw.bank.amount.state.formatFiat
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.success.ui.TangemPayBankWithdrawSuccessScreen
import kotlinx.coroutines.flow.StateFlow
import java.util.Currency

/** Closing state of the wizard: the order is signed and the backend is moving the money. */
internal class TangemPayBankWithdrawSuccessComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        val draft by params.draft.collectAsStateWithLifecycle()
        BackHandler(onBack = params.onDoneClick)
        TangemPayBankWithdrawSuccessScreen(
            amount = draft.sentAmount(),
            onDoneClick = params.onDoneClick,
            modifier = modifier,
        )
    }

    private fun BankWithdrawDraft.sentAmount(): String? {
        val amount = amount ?: return null
        val currencyCode = destination?.fee?.currency ?: return null
        val currency = runCatching { Currency.getInstance(currencyCode) }.getOrNull() ?: return null
        return amount.formatFiat(currency)
    }

    data class Params(
        val draft: StateFlow<BankWithdrawDraft>,
        val onDoneClick: () -> Unit,
    )
}