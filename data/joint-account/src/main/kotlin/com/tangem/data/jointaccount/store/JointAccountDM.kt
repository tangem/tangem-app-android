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
    val operations: List<OperationDM> = emptyList(),
) {

    @Serializable
    internal data class MemberDM(
        val name: String,
        val address: String,
        val role: String,
        val inviteId: String? = null,
    )

    @Serializable
    internal data class OperationDM(
        val operationId: String,
        val chainId: Long,
        val status: String,
        val initiator: String?,
        val signatures: List<SignatureDM>,
        val createdAt: String? = null,
        val to: String,
        val value: String,
        val data: String,
        val operation: Int,
        val safeTxGas: String,
        val baseGas: String,
        val gasPrice: String,
        val gasToken: String,
        val refundReceiver: String,
        val nonce: Long,
    )

    @Serializable
    internal data class SignatureDM(
        val address: String,
        val state: String,
        val at: String? = null,
    )
}