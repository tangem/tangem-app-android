package com.tangem.domain.jointaccount.model

sealed interface JointAccountInviteIssueError {

    val cause: Throwable

    /** The slot is taken, the account left `pending`, or this wallet is not the creator: the list is stale */
    data class SlotUnavailable(override val cause: Throwable) : JointAccountInviteIssueError

    /** Anything else, safe to retry */
    data class Failed(override val cause: Throwable) : JointAccountInviteIssueError
}