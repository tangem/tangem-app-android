package com.tangem.domain.jointaccount.model

/**
 * Why the confirmation flow stopped. The shared cases of the one-tap flows live in [JointAccountFlowError];
 * only the confirmation-specific ones are declared here.
 */
sealed interface JointAccountConfirmationError : JointAccountFlowError {

    /** The wallet has no joint account matching the row being confirmed */
    data object AccountNotFound : JointAccountConfirmationError

    /**
     * The account cannot be confirmed yet: it is not `confirming`, a slot is still free, or the backend has
     * not returned the Safe address
     */
    data object NotReadyForConfirmation : JointAccountConfirmationError

    /**
     * The backend's Safe address matches the CREATE2 address of the composition under no supported singleton —
     * the account must not be confirmed, the user is directed to support
     */
    data object AddressMismatch : JointAccountConfirmationError

    /**
     * The tapped wallet's owner key belongs to none of the members — the backend would reject the signature
     * with 403 anyway, so the call is not made
     */
    data object NotMember : JointAccountConfirmationError
}