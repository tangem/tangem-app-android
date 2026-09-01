package com.tangem.domain.jointaccount.model

/** Errors of the activation flow. */
sealed interface JointAccountActivationError {

    /** The user closed the NFC session — the flow stops silently, there is nothing to report */
    data object UserCancelled : JointAccountActivationError

    /** The wallet has no joint account matching the row being activated */
    data object AccountNotFound : JointAccountActivationError

    /**
     * The account cannot be activated yet: it is not `confirming`, a slot is still free, or the backend has
     * not returned the Safe address
     */
    data object NotReadyForActivation : JointAccountActivationError

    /**
     * The backend's Safe address matches the CREATE2 address of the composition under no supported singleton —
     * the account must not be activated, the user is directed to support
     */
    data object AddressMismatch : JointAccountActivationError

    data class Failed(val cause: Throwable) : JointAccountActivationError
}