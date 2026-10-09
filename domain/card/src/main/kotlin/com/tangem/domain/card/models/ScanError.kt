package com.tangem.domain.card.models

import com.tangem.common.core.TangemError

/** Unsuccessful outcome of a card scan */
sealed interface ScanError {

    /** The card has no wallet (or the twins welcome wasn't shown); the app has already opened onboarding */
    data object WalletNotCreated : ScanError

    /** The card failed attestation; the warning dialog was shown and the user has closed it */
    data object CardVerificationFailed : ScanError

    /** Any other card SDK error, including user cancellation */
    data class Failure(val error: TangemError) : ScanError
}