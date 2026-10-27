package com.tangem.data.txhistory.repository.converter

import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationRecord
import com.tangem.store.datasource.txhistory.db.entity.joint.JointOperationWithVotes
import com.tangem.utils.converter.Converter

/** A status this build does not know is kept in flight, as the joint account list does */
internal class JointOperationRecordConverter(
    private val finishedConverter: FinishedJointOperationConverter,
) : Converter<JointOperationWithVotes, JointOperationRecord> {

    override fun convert(value: JointOperationWithVotes): JointOperationRecord {
        finishedConverter.convert(value)?.let { return JointOperationRecord.Finished(it) }

        val operation = value.operation
        return JointOperationRecord.InFlight(
            JointOperation(
                operationId = operation.operationId,
                chainId = operation.chainId,
                safeTx = operation.safeTx.toSafeTx(),
                status = when (operation.status) {
                    STATUS_AWAITING_SIGNATURES -> JointOperation.Status.AWAITING_SIGNATURES
                    STATUS_PENDING -> JointOperation.Status.PENDING
                    else -> JointOperation.Status.UNKNOWN
                },
                initiator = operation.initiator,
                signatures = value.votes.toSignatures(),
                createdAt = operation.createdAt,
            ),
        )
    }

    private companion object {
        const val STATUS_AWAITING_SIGNATURES = "awaiting_signatures"
        const val STATUS_PENDING = "pending"
    }
}