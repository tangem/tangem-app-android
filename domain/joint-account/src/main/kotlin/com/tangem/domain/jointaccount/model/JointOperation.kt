package com.tangem.domain.jointaccount.model

/**
 * A non-terminal Safe operation of a joint account. Terminal operations never exist in the domain: the list
 * endpoint stops returning an operation once it finishes, so its absence — together with the on-chain history —
 * is how completion is observed, and the (account, chain) slot frees itself structurally.
 *
 * @property operationId the backend's uuid, the path parameter of the sign and reject endpoints
 * @property chainId     the EVM network the operation lives in
 * @property safeTx      the full signed structure; everything shown to the user is decoded from it, never from
 * separate response fields
 * @property status      [Status.UNKNOWN] is a backend value this build does not know — treated as in-flight
 * (the slot stays busy), never sent back

 * backend did not report one

 * @property signatures  only the members who responded; the rest of the roster is drawn from
 * [JointAccount.members] and rendered as waiting
 */
data class JointOperation(
    val operationId: String,
    val chainId: Long,
    val safeTx: SafeTx,
    val status: Status,
    val initiator: String?,
    val signatures: List<MemberSignature>,
    val createdAt: String? = null,
) {

    enum class Status {
        AWAITING_SIGNATURES,
        PENDING,
        UNKNOWN,
    }

    /**
     * @property address the responding member's owner address, lowercase
     * @property state   [State.UNKNOWN] is a backend value this build does not know
     * @property at      when the member responded, ISO-8601 as the backend sent it
     */
    data class MemberSignature(
        val address: String,
        val state: State,
        val at: String?,
    ) {

        enum class State {
            SIGNED,
            DECLINED,
            UNKNOWN,
        }
    }
}