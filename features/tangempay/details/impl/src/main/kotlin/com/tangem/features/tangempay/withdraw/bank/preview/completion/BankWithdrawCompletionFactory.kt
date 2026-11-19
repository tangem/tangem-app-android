package com.tangem.features.tangempay.withdraw.bank.preview.completion

import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpQuote
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import javax.inject.Inject

internal class BankWithdrawCompletionFactory @Inject constructor(
    private val orderCompletionFactory: OrderBankWithdrawCompletion.Factory,
    private val routeCompletionFactory: RouteBankWithdrawCompletion.Factory,
    private val speiCompletionFactory: SpeiBankWithdrawCompletion.Factory,
) {

    fun create(
        sourceAccount: BankWithdrawSourceAccount,
        scope: CoroutineScope,
        callbacks: Callbacks,
    ): BankWithdrawCompletion = when (sourceAccount) {
        is BankWithdrawSourceAccount.Payment -> orderCompletionFactory.create(
            userWalletId = sourceAccount.userWalletId,
            scope = scope,
            onOrderCreated = callbacks.onOrderCreated,
        )
        is BankWithdrawSourceAccount.Money -> sourceAccount.productInstanceId?.let { productInstanceId ->
            MoneyAccountBankWithdrawCompletion(
                route = routeCompletionFactory.create(
                    userWalletId = sourceAccount.userWalletId,
                    productInstanceId = productInstanceId,
                    onDestinationChanged = callbacks.onDestinationChanged,
                    onNetworkFeeChanged = callbacks.onNetworkFeeChanged,
                    newRecipientListener = callbacks.newRecipientListener,
                ),
                spei = speiCompletionFactory.create(
                    userWalletId = sourceAccount.userWalletId,
                    productInstanceId = productInstanceId,
                    onNetworkFeeChanged = callbacks.onNetworkFeeChanged,
                    onQuoteExpired = callbacks.onQuoteExpired,
                    newRecipientListener = callbacks.newRecipientListener,
                ),
            )
        } ?: UnavailableBankWithdrawCompletion
    }

    data class Callbacks(
        val onOrderCreated: (BankOfframpOrderResult) -> Unit,
        val onDestinationChanged: (OfframpDestination) -> Unit,
        val onNetworkFeeChanged: (BigDecimal) -> Unit,
        val onQuoteExpired: () -> Unit,
        val newRecipientListener: NewRecipientListener,
    )
}

private class MoneyAccountBankWithdrawCompletion(
    private val route: BankWithdrawCompletion,
    private val spei: BankWithdrawCompletion,
) : BankWithdrawCompletion {

    override val quote: StateFlow<BankOfframpQuote?> = spei.quote

    override suspend fun prepare(draft: BankWithdrawDraft, onState: (CompletionState) -> Unit) {
        forRail(draft).prepare(draft, onState)
    }

    override suspend fun complete(draft: BankWithdrawDraft, onState: (CompletionState) -> Unit) {
        forRail(draft).complete(draft, onState)
    }

    private fun forRail(draft: BankWithdrawDraft): BankWithdrawCompletion =
        if (draft.destination?.rail == BankRail.SPEI) spei else route
}

private object UnavailableBankWithdrawCompletion : BankWithdrawCompletion {

    override suspend fun complete(draft: BankWithdrawDraft, onState: (CompletionState) -> Unit) {
        onState(CompletionState.Failed(PreviewFailure.UnsupportedConfiguration))
    }
}