package com.tangem.domain.jointaccount.model

/** Outcome of redeeming an invite, as the data layer reports it. */
sealed interface JointAccountJoinResult {

    /** The slot is taken and the backend returned the account */
    data class Joined(val account: JointAccount) : JointAccountJoinResult

    /**
     * 409 — the invite is spent, or this owner address is already a member. The response does not tell the two
     * apart, so the caller resolves it by re-reading the list: finding itself among the members means the join
     * had already happened, anything else means the invite is dead.
     */
    data object Conflict : JointAccountJoinResult
}