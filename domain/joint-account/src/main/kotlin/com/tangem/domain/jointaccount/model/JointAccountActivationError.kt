package com.tangem.domain.jointaccount.model

/**
 * Why the activation flow stopped. The shared cases of the one-tap flows live in [JointAccountFlowError];
 * only the activation-specific ones are declared here.
 */
sealed interface JointAccountActivationError : JointAccountFlowError {

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

    /**
     * The tapped wallet's owner key is not the creator's — only the creator activates, and the backend would
     * reject the signature with 403 anyway, so the call is not made
     */
    data object NotCreator : JointAccountActivationError
}