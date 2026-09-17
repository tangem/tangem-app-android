package com.tangem.domain.jointaccount.model

/** Why cancelling (initiator) or declining (co-signer) a joint operation stopped */
sealed interface JointOperationRejectError {

    data object OperationNotFound : JointOperationRejectError

    /** The threshold was reached first, the operation is already being executed */
    data object ThresholdReached : JointOperationRejectError

    /** The signature recovers to an address that is not a member of the account */
    data object NotAMember : JointOperationRejectError

    data class Failed(val cause: Throwable) : JointOperationRejectError
}