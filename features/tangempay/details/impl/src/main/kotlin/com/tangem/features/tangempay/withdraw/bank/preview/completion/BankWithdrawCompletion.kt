package com.tangem.features.tangempay.withdraw.bank.preview.completion

import com.tangem.domain.tangempay.model.bankofframp.BankAccountDetails
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpQuote
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface BankWithdrawCompletion {

    val quote: StateFlow<BankOfframpQuote?>
        get() = NO_QUOTE

    suspend fun prepare(draft: BankWithdrawDraft, onState: (CompletionState) -> Unit) {}

    suspend fun complete(draft: BankWithdrawDraft, onState: (CompletionState) -> Unit)
}

internal sealed interface CompletionState {

    data object Idle : CompletionState

    data object Sending : CompletionState

    data object Completed : CompletionState

    data object WithdrawalInProgress : CompletionState

    data class Failed(
        val failure: PreviewFailure,
        val errorCode: Int? = null,
        val isQuoteFailure: Boolean = false,
    ) : CompletionState
}

internal interface NewRecipientListener {

    fun onSaved(recipient: BankAccountDetails)

    fun onRejected(recipient: BankAccountDetails, errorCode: Int?)
}

private val NO_QUOTE: StateFlow<BankOfframpQuote?> = MutableStateFlow(null)

internal fun BankOfframpError.isRecipientRefusal(): Boolean = when (this) {
    BankOfframpError.BadRequest,
    BankOfframpError.RecipientRegionUnsupported,
    BankOfframpError.BusinessUnsupported,
    -> true
    else -> false
}