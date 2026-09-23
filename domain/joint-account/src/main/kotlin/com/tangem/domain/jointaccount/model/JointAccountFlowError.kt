package com.tangem.domain.jointaccount.model

import com.tangem.domain.models.account.DerivationIndex

/**
 * The shared frame of the one-tap flows (create, join, activate): every flow signs a payload with the card and
 * calls the backend, so cancelling the session and a plain failure mean the same thing in each of them. The
 * cases below implement exactly the flow interfaces they apply to, so a `when` over a flow's own error type
 * stays exhaustive and never sees another flow's cases.
 */
sealed interface JointAccountFlowError {

    /**
     * The user dismissed the NFC session. Not a failure: the flow stops silently, its form keeps the entered

     */
    data object UserCancelled :
        JointAccountCreationError,
        JointAccountJoinError,
        JointAccountActivationError

    /**
     * The backend reported an owner derivation index that cannot exist — the counter came back negative.
     * Nothing to sign under, so the flow stops instead of crashing on a failed requirement. Activation never
     * sees it: its index is the account's own [com.tangem.domain.models.account.OwnerKeyIndex].
     */
    data class InvalidDerivationIndex(val cause: DerivationIndex.Error) :
        JointAccountCreationError,
        JointAccountJoinError

    /** Anything else: no network, a signature the backend rejected, a card error. */
    data class Failed(val cause: Throwable) :
        JointAccountCreationError,
        JointAccountJoinError,
        JointAccountActivationError
}