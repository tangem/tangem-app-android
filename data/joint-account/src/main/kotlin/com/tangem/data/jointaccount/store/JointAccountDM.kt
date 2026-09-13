package com.tangem.data.jointaccount.store

import kotlinx.serialization.Serializable

/**
 * Persisted form of a joint account. `status` and `role` are strings rather than enums so that a value written by
 * a newer build never breaks deserialization in an older one. `StatusSource` is deliberately not persisted —
 * restored data is always `CACHE`.
 */
@Serializable
internal data class JointAccountDM(
    val cryptoAccountId: String,
    val membersCount: Int,
    val threshold: Int,
    val safeAddress: String?,
    val status: String,
    val members: List<MemberDM>,
) {

    @Serializable
    internal data class MemberDM(
        val name: String,
        val address: String,
        val role: String,
    )
}