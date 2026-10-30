package com.tangem.domain.collectibles

import com.tangem.core.error.UniversalError

sealed interface CollectiblesAccountError {

    /** The feature is off, or there is no wallet to use. */
    data object Unsupported : CollectiblesAccountError

    /** No delegate yet — call `CollectiblesHotDelegate.provision` first. */
    data object DelegateNotProvisioned : CollectiblesAccountError

    data class DelegateKeyCreationFailed(val cause: Throwable) : CollectiblesAccountError

    data class DelegateSigningFailed(val cause: Throwable) : CollectiblesAccountError

    /** The user dismissed the card session or the access code prompt. */
    data object Cancelled : CollectiblesAccountError

    /** The card refused; the NFC sheet has already shown it. */
    data class CardError(val code: Int, val message: String) : CollectiblesAccountError

    /** The backend did not take the account record, so the account is not created on this device either. */
    data class RegistrationFailed(val cause: Throwable) : CollectiblesAccountError

    /** Ours or the chain's, not shown to the user yet. */
    data class Vault(val error: UniversalError) : CollectiblesAccountError
}