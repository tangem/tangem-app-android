package com.tangem.domain.jointaccount.model

sealed interface JointOperationFetchResult {

    /** The operation is stored; [eTag] is the tag to send with the next fetch, `null` when the backend sent none */
    data class Updated(val eTag: String?) : JointOperationFetchResult

    /** The operation has not changed since the given tag */
    data object NotModified : JointOperationFetchResult

    /** The backend does not know the operation or refuses its id, or the wallet lost its slot in the account */
    data object Gone : JointOperationFetchResult
}