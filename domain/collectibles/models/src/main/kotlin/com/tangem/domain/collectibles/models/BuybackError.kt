package com.tangem.domain.collectibles.models

sealed interface BuybackError {

    /** The backend will not buy this card back. */
    data object NotAllowed : BuybackError

    /** The user dismissed the card session or the access code prompt. */
    data object Cancelled : BuybackError

    /** The card refused; the NFC sheet has already shown it. */
    data object CardRefused : BuybackError

    data object Unexpected : BuybackError

    /** Sent, but not confirmed in the wait; a retry is safe, the card can leave only once. */
    data object NotConfirmed : BuybackError
}