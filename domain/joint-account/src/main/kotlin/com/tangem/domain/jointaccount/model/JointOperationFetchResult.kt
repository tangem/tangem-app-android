package com.tangem.domain.jointaccount.model

sealed interface JointOperationFetchResult {

    data object Updated : JointOperationFetchResult

    data object InFlight : JointOperationFetchResult

    /** The operation has not changed since the previous fetch */
    data object NotModified : JointOperationFetchResult

    /** The backend does not know the operation or refuses its id, or the wallet lost its slot in the account */
    data object Gone : JointOperationFetchResult
}