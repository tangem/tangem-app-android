package com.tangem.data.txhistory.repository.converter

import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationEntity
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationVoteEntity
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationWithVotes
import com.tangem.utils.converter.Converter
import java.math.BigInteger
import java.time.Instant

/** `null` for an operation that is still in flight, or whose status this build does not know */
internal class FinishedJointOperationConverter : Converter<JointOperationWithVotes, FinishedJointOperation?> {

    override fun convert(value: JointOperationWithVotes): FinishedJointOperation? {
        val operation = value.operation
        val status = finishedStatus(operation.status) ?: return null
        val finishedAt = operation.finishedAt ?: return null

        return FinishedJointOperation(
            operationId = operation.operationId,
            chainId = operation.chainId,
            safeAddress = operation.safeAddress,
            safeTx = operation.safeTx.toSafeTx(),
            status = status,
            initiator = operation.initiator,
            signatures = value.votes.toSignatures(),
            createdAt = operation.createdAt,
            finishedAtMillis = Instant.parse(finishedAt).toEpochMilli(),
            txHash = operation.txHash,
        )
    }

    private fun finishedStatus(value: String): FinishedJointOperation.Status? = when (value) {
        STATUS_EXECUTED -> FinishedJointOperation.Status.EXECUTED
        STATUS_FAILED -> FinishedJointOperation.Status.FAILED
        STATUS_CANCELLED -> FinishedJointOperation.Status.CANCELLED
        else -> null
    }

    companion object {
        private const val STATUS_EXECUTED = "executed"
        private const val STATUS_FAILED = "failed"
        private const val STATUS_CANCELLED = "cancelled"

        val FINISHED_STATUSES = listOf(STATUS_EXECUTED, STATUS_FAILED, STATUS_CANCELLED)
    }
}

internal fun JointOperationEntity.SafeTxEmbedded.toSafeTx() = SafeTx(
    to = to.lowercase(),
    value = BigInteger(value),
    data = data.lowercase(),
    operation = operation,
    safeTxGas = BigInteger(safeTxGas),
    baseGas = BigInteger(baseGas),
    gasPrice = BigInteger(gasPrice),
    gasToken = gasToken.lowercase(),
    refundReceiver = refundReceiver.lowercase(),
    nonce = BigInteger(nonce),
)

/** In the order the members answered */
internal fun List<JointOperationVoteEntity>.toSignatures(): List<JointOperation.MemberSignature> =
    sortedBy(JointOperationVoteEntity::votedAt).map { vote ->
        JointOperation.MemberSignature(
            address = vote.address,
            state = when (vote.vote) {
                VOTE_APPROVE -> JointOperation.MemberSignature.State.SIGNED
                VOTE_REJECT -> JointOperation.MemberSignature.State.DECLINED
                else -> JointOperation.MemberSignature.State.UNKNOWN
            },
            at = vote.votedAt,
        )
    }

private const val VOTE_APPROVE = "approve"
private const val VOTE_REJECT = "reject"