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
}