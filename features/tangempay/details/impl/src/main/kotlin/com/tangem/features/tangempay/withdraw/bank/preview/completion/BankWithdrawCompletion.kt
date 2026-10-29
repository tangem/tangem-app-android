package com.tangem.features.tangempay.withdraw.bank.preview.completion

import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure

internal interface BankWithdrawCompletion {

    suspend fun complete(draft: BankWithdrawDraft, onState: (CompletionState) -> Unit)
}

internal sealed interface CompletionState {

    data object Idle : CompletionState

    data object Sending : CompletionState

    data object Completed : CompletionState

    data object WithdrawalInProgress : CompletionState

    data class Failed(val failure: PreviewFailure, val errorCode: Int? = null) : CompletionState
}