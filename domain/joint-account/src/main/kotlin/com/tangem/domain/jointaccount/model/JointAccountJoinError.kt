package com.tangem.domain.jointaccount.model

/**
 * Why joining did not produce an account. The shared cases of the one-tap flows live in
 * [JointAccountFlowError]; only the join-specific ones are declared here.
 */
sealed interface JointAccountJoinError : JointAccountFlowError {

    /**
     * The invite cannot be redeemed: it is spent, or the config no longer matches the account. Terminal for
     * this link — a retry can only fail the same way.
     */
    data object InvalidInvite : JointAccountJoinError
}