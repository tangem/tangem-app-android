package com.tangem.features.tangempay.withdraw.bank.preview.model

import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.features.tangempay.withdraw.bank.preview.completion.CompletionState

internal data class PreviewState(
    val completion: CompletionState = CompletionState.Idle,
    /** A cold wallet signs the transfer with a card tap; a hot one signs without a card. */
    val isColdWallet: Boolean = true,
    val isBalanceHidden: Boolean = false,
) {

    val isSending: Boolean get() = completion == CompletionState.Sending

    val failure: PreviewFailure? get() = (completion as? CompletionState.Failed)?.failure
}

/** Outcomes that replace the summary with a full-screen result; only [General] offers Retry. */
internal enum class PreviewFailure {
    RecipientRegion,
    SenderRegion,
    Business,
    UnsupportedConfiguration,
    General,
    TransferOutcomeUnknown,
}

internal fun BankOfframpError.toFailure(): PreviewFailure = when (this) {
    BankOfframpError.RecipientRegionUnsupported -> PreviewFailure.RecipientRegion
    BankOfframpError.SenderRegionUnsupported -> PreviewFailure.SenderRegion
    BankOfframpError.BusinessUnsupported -> PreviewFailure.Business
    BankOfframpError.UnsupportedConfiguration,
    is BankOfframpError.Unavailable,
    -> PreviewFailure.UnsupportedConfiguration
    BankOfframpError.Validation,
    BankOfframpError.PollingTimeout,
    BankOfframpError.WithdrawalInProgress,
    is BankOfframpError.Unknown,
    -> PreviewFailure.General
}