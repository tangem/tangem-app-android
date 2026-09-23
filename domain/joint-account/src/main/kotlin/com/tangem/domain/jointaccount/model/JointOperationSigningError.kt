package com.tangem.domain.jointaccount.model

/** Why signing a Safe operation stopped. */
sealed interface JointOperationSigningError {

    /** The user dismissed the signing session — the flow stops silently */
    data object UserCancelled : JointOperationSigningError

    /**
     * No derived owner key at the account's index — for an active joint account this means the derived
     * keys were lost, not never made.
     */
    data object OwnerKeyNotDerived : JointOperationSigningError

    /** Anything else: a missing wallet, a card error, an unexpected signer failure. */
    data class Failed(val cause: Throwable) : JointOperationSigningError
}