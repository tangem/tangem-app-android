package com.tangem.domain.jointaccount.model

/**
 * A joint account operation that has ended. Unlike [JointOperation], it never holds the network slot.
 *
 * @property operationId the backend's uuid
 * @property chainId     the EVM network the operation lived in
 * @property safeAddress the Safe that ran the operation, lowercase
 * @property safeTx      what the operation moved; everything shown to the user is decoded from it
 * @property status      how it ended

 * @property signatures  only the members who responded; the rest of the roster did not vote

 * @property finishedAtMillis when the operation got its result or was cancelled
 * @property txHash      the relayer transaction; `null` for [Status.CANCELLED] and for a [Status.FAILED] one that
 * never reached the chain
 */
data class FinishedJointOperation(
    val operationId: String,
    val chainId: Long,
    val safeAddress: String,
    val safeTx: SafeTx,
    val status: Status,
    val initiator: String?,
    val signatures: List<JointOperation.MemberSignature>,
    val createdAt: String,
    val finishedAtMillis: Long,
    val txHash: String?,
) {

    enum class Status {
        EXECUTED,
        FAILED,
        CANCELLED,
    }
}