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
        signatures = value.signatures.map { signature ->
            JointOperation.MemberSignature(
                address = signature.address.lowercase(),
                state = convertSignatureState(signature.state),
                at = signature.at,
            )
        },
    )

    fun convertInFlight(dtos: List<JointOperationDto>): Map<Long, JointOperation> {
        return buildMap {
            dtos.forEach { dto ->
                val operation = convert(dto)

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

    private fun convertSignatureState(value: String): JointOperation.MemberSignature.State = when (value) {
        "signed" -> JointOperation.MemberSignature.State.SIGNED
        "declined" -> JointOperation.MemberSignature.State.DECLINED
        else -> JointOperation.MemberSignature.State.UNKNOWN
    }
}