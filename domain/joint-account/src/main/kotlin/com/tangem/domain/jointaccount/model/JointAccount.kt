package com.tangem.domain.jointaccount.model

import com.tangem.domain.models.StatusSource

/**
 * A joint account the wallet participates in, as the backend returns it on stage 1.
 *
 * @property cryptoAccountId the wallet's own row in `accounts` (64 hex chars) — the merge key against
 * `Account.CryptoPortfolio`-style rows, which carry the personal name, icon, color and tokens
 * @property membersCount    slots including the creator, fixed at creation
 * @property threshold       signatures required to execute an operation, fixed at creation
 * @property safeAddress     the Safe address, one and the same in every supported EVM network; `null` while pending
 * @property status          account lifecycle state, forward only
 * @property members         taken slots, ordered by join time, the creator first. The app finds itself in the list
 * by comparing [Member.address] against the address derived from the card (lowercased), never by trusting the
 * backend. Free slots are `membersCount - members.size`
 * @property invites         the issued and unredeemed invites of a pending account, without their codes. Only the
 * creator receives them, and only while the account is pending; everyone else gets an empty list
 * @property source          freshness of this data: [StatusSource.ACTUAL] right after a successful fetch,
 * [StatusSource.CACHE] when restored from persistence, [StatusSource.ONLY_CACHE] when a refresh failed
 * @property inFlightOperations the account's non-terminal operations keyed by chain — at most one per network
 * by the slot invariant. A busy key blocks creating another operation in that network; the key frees itself
 * when the operation stops arriving from the backend
 */
data class JointAccount(
    val cryptoAccountId: String,
    val membersCount: Int,
    val threshold: Int,
    val safeAddress: String?,
    val status: Status,
    val members: List<Member>,
    val source: StatusSource,
    val invites: List<JointAccountInvite> = emptyList(),
    val inFlightOperations: Map<Long, JointOperation> = emptyMap(),
) {

    /**
     * The backend fills [Member.slot] only in the list it returns to the creator of a pending account. Who the creator
     * is comes from [Member.role]; this only says whether the slot numbers are known.
     */
    val hasRevealedSlots: Boolean
        get() = members.any { it.slot != null }

    /** Slot numbers free to invite into: `1..membersCount - 1` minus the taken ones. Empty unless [hasRevealedSlots] */
    val freeSlots: List<Int>
        get() {
            if (!hasRevealedSlots) return emptyList()
            val taken = members.mapNotNullTo(hashSetOf()) { it.slot }
            return (FIRST_INVITABLE_SLOT until membersCount).filterNot { it in taken }
        }

    /**
     * [CANCELLED] is reserved by the contract — nothing sets it on this stage.
     * [UNKNOWN] is a backend value this build does not know — displayed conservatively, never sent back.
     */
    enum class Status {
        PENDING,
        CONFIRMING,
        ACTIVE,
        CANCELLED,
        UNKNOWN,
    }

    /**
     * @property name     the member's display name, 1..25 chars
     * @property address  the member's owner address in the Safe, lower-case hex — compare case-insensitively
     * @property role     only the creator sends invites
     * @property slot     `0` for the creator, the slot the member joined through otherwise; `null` whenever the
     * backend withholds it — see [hasRevealedSlots]
     * @property signature the member's confirmation signature, lower-case hex; `null` until the member confirms
     */
    data class Member(
        val name: String,
        val address: String,
        val role: Role,
        val slot: Int? = null,
        val signature: String? = null,
    ) {

        val isVerified: Boolean
            get() = signature != null
    }

    /** [UNKNOWN] is a backend value this build does not know. */
    enum class Role {
        CREATOR,
        MEMBER,
        UNKNOWN,
    }

    companion object {
        const val CREATOR_SLOT = 0
        const val FIRST_INVITABLE_SLOT = 1
    }
}