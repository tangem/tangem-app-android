package com.tangem.features.tangempay.withdraw.bank.preview.completion

import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal class BankWithdrawCompletionFactory @Inject constructor(
    private val orderCompletionFactory: OrderBankWithdrawCompletion.Factory,
) {

    fun create(
        sourceAccount: BankWithdrawSourceAccount,
        scope: CoroutineScope,
        onOrderCreated: (BankOfframpOrderResult) -> Unit,
    ): BankWithdrawCompletion = when (sourceAccount) {
        is BankWithdrawSourceAccount.Payment -> orderCompletionFactory.create(
            userWalletId = sourceAccount.userWalletId,
            scope = scope,
            onOrderCreated = onOrderCreated,
        )
        is BankWithdrawSourceAccount.Money -> UnavailableBankWithdrawCompletion
    }
}

private object UnavailableBankWithdrawCompletion : BankWithdrawCompletion {

    override suspend fun complete(draft: BankWithdrawDraft, onState: (CompletionState) -> Unit) {
        onState(CompletionState.Failed(PreviewFailure.UnsupportedConfiguration))
    }
}