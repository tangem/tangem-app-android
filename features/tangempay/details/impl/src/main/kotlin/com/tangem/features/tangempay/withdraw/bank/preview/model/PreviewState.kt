package com.tangem.features.tangempay.withdraw.bank.preview.model

import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpQuote
import com.tangem.features.tangempay.withdraw.bank.preview.completion.CompletionState

internal data class PreviewState(
    val completion: CompletionState = CompletionState.Idle,
    /** A cold wallet signs the transfer with a card tap; a hot one signs without a card. */
    val isColdWallet: Boolean = true,
    val isBalanceHidden: Boolean = false,
    val quote: LockedQuote? = null,
) {

    val isSending: Boolean get() = completion == CompletionState.Sending

    val failure: PreviewFailure? get() = (completion as? CompletionState.Failed)?.failure
}

/**
 * [secondsLeft] counts down to the moment the backend stops accepting a transfer on [quote]; it is null when the
 * device clock gave the quote less than [MIN_TRUSTED_LOCK_MILLIS] on arrival and so cannot be trusted to time it.
 */
internal data class LockedQuote(val quote: BankOfframpQuote, val secondsLeft: Long?)

/**
 * A quote comes with about fifteen minutes; much less on arrival means the device clock runs ahead, so the app
 * neither counts down nor re-quotes on its own, and the backend's 140164 decides when the quote is gone.
 */
internal const val MIN_TRUSTED_LOCK_MILLIS = 60_000L

internal fun BankOfframpQuote.isLockTrusted(nowMillis: Long): Boolean =
    sendDeadline.millis - nowMillis >= MIN_TRUSTED_LOCK_MILLIS

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
    BankOfframpError.QuoteExpired,
    BankOfframpError.VerificationIncomplete,
    BankOfframpError.Conflict,
    BankOfframpError.AmountOutOfLimits,
    BankOfframpError.Rejected,
    is BankOfframpError.Unknown,
    -> PreviewFailure.General
}