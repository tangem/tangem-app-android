package com.tangem.data.jointaccount.converter

import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.store.datasource.jointaccount.models.JointOperationDto
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class JointOperationConverter @Inject constructor(
    private val safeTxConverter: SafeTxConverter,
) : Converter<JointOperationDto, JointOperation> {

    override fun convert(value: JointOperationDto): JointOperation = JointOperation(
        operationId = value.operationId,
        chainId = value.chainId,
        safeTx = safeTxConverter.convert(value.safeTx),
        status = convertStatus(value.status),
        initiator = value.initiator?.lowercase(),
        createdAt = value.createdAt,
        signatures = value.votes.map { vote ->
            JointOperation.MemberSignature(
                address = vote.address.lowercase(),
                state = convertVote(vote.vote),
                at = vote.votedAt,
            )
        },
    )

    /** One operation the backend spells out of contract is dropped alone; the account and its other slots stay */
    fun convertInFlight(dtos: List<JointOperationDto>): Map<Long, JointOperation> {
        return buildMap {
            dtos.forEach { dto ->
                val operation = runCatching { convert(dto) }
                    .onFailure { TangemLogger.e("Dropped the joint operation ${dto.operationId}", it) }
                    .getOrNull()
                    ?: return@forEach

                if (putIfAbsent(operation.chainId, operation) != null) {
                    TangemLogger.e(
                        "Duplicate in-flight joint operation on chain ${operation.chainId}: " +
                            "kept the first by order, dropped ${operation.operationId}",
                    )
                }
            }
        }
    }

    /** An unknown status keeps the operation in flight: blocking the slot is safer than a duplicate */
    private fun convertStatus(value: String): JointOperation.Status = when (value) {
        "awaiting_signatures" -> JointOperation.Status.AWAITING_SIGNATURES
        "pending" -> JointOperation.Status.PENDING
        else -> JointOperation.Status.UNKNOWN
    }

    private fun convertVote(value: String): JointOperation.MemberSignature.State = when (value) {
        "approve" -> JointOperation.MemberSignature.State.SIGNED
        "reject" -> JointOperation.MemberSignature.State.DECLINED
        else -> JointOperation.MemberSignature.State.UNKNOWN
    }
}