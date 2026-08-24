package com.tangem.domain.jointaccount.model

/**
 * Outcome of registering a joint account on the backend.
 *
 * The conflict is a result rather than an error because it is a normal, recoverable branch of the creation
 * flow: the account behind the creator's owner address already exists — a lost response of a previous
 * attempt, or a second device of the same wallet. The caller resolves it by re-reading the wallet's joint
 * accounts and opening the one that lists its own owner address; it must never retry under the next
 * derivation index, which the contract forbids.
 */
sealed interface JointAccountCreationResult {

    data class Created(val account: JointAccount) : JointAccountCreationResult

    data object CreatorAlreadyRegistered : JointAccountCreationResult
}