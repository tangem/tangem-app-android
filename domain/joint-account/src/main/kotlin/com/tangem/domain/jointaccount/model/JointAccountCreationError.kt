package com.tangem.domain.jointaccount.model

/**
 * Why creating a joint account did not produce an account. The shared cases of the one-tap flows live in
 * [JointAccountFlowError]; only the creation-specific ones are declared here.
 */
sealed interface JointAccountCreationError : JointAccountFlowError {

    /**
     * The backend reports the creator's owner address as already registered, but no account with that address
     * came back in the list. Nothing left to open — the user is asked to contact support.
     */
    data object ExistingAccountNotFound : JointAccountCreationError

    /**
     * The request failed and the wallet already takes part in the maximum number of joint accounts. The backend
     * reports the limit with the same 400 as a malformed request, so the refreshed list is what tells them apart.
     */
    data object AccountsLimitReached : JointAccountCreationError
}