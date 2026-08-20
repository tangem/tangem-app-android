package com.tangem.domain.jointaccount.model

/**
 * Why creating a joint account did not produce an account.
 */
sealed interface JointAccountCreationError {

    /**
     * The user dismissed the NFC session. Not a failure: the creation form is shown again with its values,

     */
    data object UserCancelled : JointAccountCreationError

    /**
     * The backend reports the creator's owner address as already registered, but no account with that address
     * came back in the list. Nothing left to open — the user is asked to contact support.
     */
    data object ExistingAccountNotFound : JointAccountCreationError

    /** Anything else: no network, a signature the backend rejected, a card error. */
    data class Failed(val cause: Throwable) : JointAccountCreationError
}