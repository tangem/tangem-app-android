package com.tangem.data.jointaccount.converter

import com.tangem.data.jointaccount.store.JointAccountDM
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.StatusSource
import com.tangem.utils.converter.TwoWayConverter
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain ↔ persisted form. Restored data is always [StatusSource.CACHE]. A status the build does not know is
 * persisted as `"unknown"` — it shows as [JointAccount.Status.UNKNOWN] until the next fetch, same as it did live.
 */
@Singleton
internal class JointAccountDMConverter @Inject constructor() : TwoWayConverter<JointAccount, JointAccountDM> {

    override fun convert(value: JointAccount): JointAccountDM = JointAccountDM(
        cryptoAccountId = value.cryptoAccountId,
        membersCount = value.membersCount,
        threshold = value.threshold,
        safeAddress = value.safeAddress,
        status = value.status.toRawString(),
        members = value.members.map { member ->
            JointAccountDM.MemberDM(
                name = member.name,
                address = member.address,
                role = member.role.toRawString(),
                inviteId = member.inviteId,
            )
        },
        operations = value.inFlightOperations.values.map { operation ->
            JointAccountDM.OperationDM(
                operationId = operation.operationId,
                chainId = operation.chainId,
                status = operation.status.toRawString(),
                initiator = operation.initiator,
                createdAt = operation.createdAt,
                signatures = operation.signatures.map { signature ->
                    JointAccountDM.SignatureDM(
                        address = signature.address,
                        state = signature.state.toRawString(),
                        at = signature.at,
                    )
                },
                to = operation.safeTx.to,
                value = operation.safeTx.value.toString(),
                data = operation.safeTx.data,
                operation = operation.safeTx.operation,
                safeTxGas = operation.safeTx.safeTxGas.toString(),
                baseGas = operation.safeTx.baseGas.toString(),
                gasPrice = operation.safeTx.gasPrice.toString(),
                gasToken = operation.safeTx.gasToken,
                refundReceiver = operation.safeTx.refundReceiver,
                nonce = operation.safeTx.nonce.longValueExactCompat(),
            )
        },
    )

    override fun convertBack(value: JointAccountDM): JointAccount = JointAccount(
        cryptoAccountId = value.cryptoAccountId,
        membersCount = value.membersCount,
        threshold = value.threshold,
        safeAddress = value.safeAddress,
        status = JointAccountFieldsConverter.convertStatus(value.status),
        members = value.members.map { member ->
            JointAccount.Member(
                name = member.name,
                address = member.address,
                role = JointAccountFieldsConverter.convertRole(member.role),
                inviteId = member.inviteId,
            )
        },
        source = StatusSource.CACHE,
        inFlightOperations = value.operations
            .map(::convertOperation)
            .associateBy(JointOperation::chainId),
    )

    private fun convertOperation(dm: JointAccountDM.OperationDM): JointOperation = JointOperation(
        operationId = dm.operationId,
        chainId = dm.chainId,
        safeTx = SafeTx(
            to = dm.to,
            value = BigInteger(dm.value),
            data = dm.data,
            operation = dm.operation,
            safeTxGas = BigInteger(dm.safeTxGas),
            baseGas = BigInteger(dm.baseGas),
            gasPrice = BigInteger(dm.gasPrice),
            gasToken = dm.gasToken,
            refundReceiver = dm.refundReceiver,
            nonce = BigInteger.valueOf(dm.nonce),
        ),
        status = dm.status.toOperationStatus(),
        initiator = dm.initiator,
        createdAt = dm.createdAt,
        signatures = dm.signatures.map { signature ->
            JointOperation.MemberSignature(
                address = signature.address,
                state = signature.state.toSignatureState(),
                at = signature.at,
            )
        },
    )

    private fun JointOperation.Status.toRawString(): String = when (this) {
        JointOperation.Status.AWAITING_SIGNATURES -> "awaiting_signatures"
        JointOperation.Status.PENDING -> "pending"
        JointOperation.Status.UNKNOWN -> "unknown"
    }

    private fun String.toOperationStatus(): JointOperation.Status = when (this) {
        "awaiting_signatures" -> JointOperation.Status.AWAITING_SIGNATURES
        "pending" -> JointOperation.Status.PENDING
        else -> JointOperation.Status.UNKNOWN
    }

    private fun String.toSignatureState(): JointOperation.MemberSignature.State = when (this) {
        "signed" -> JointOperation.MemberSignature.State.SIGNED
        "declined" -> JointOperation.MemberSignature.State.DECLINED
        else -> JointOperation.MemberSignature.State.UNKNOWN
    }

    private fun JointOperation.MemberSignature.State.toRawString(): String = when (this) {
        JointOperation.MemberSignature.State.SIGNED -> "signed"
        JointOperation.MemberSignature.State.DECLINED -> "declined"
        JointOperation.MemberSignature.State.UNKNOWN -> "unknown"
    }

    private fun JointAccount.Status.toRawString(): String = when (this) {
        JointAccount.Status.PENDING -> "pending"
        JointAccount.Status.CONFIRMING -> "confirming"
        JointAccount.Status.ACTIVE -> "active"
        JointAccount.Status.CANCELLED -> "cancelled"
        JointAccount.Status.UNKNOWN -> "unknown"
    }

    private fun JointAccount.Role.toRawString(): String = when (this) {
        JointAccount.Role.CREATOR -> "creator"
        JointAccount.Role.MEMBER -> "member"
        JointAccount.Role.UNKNOWN -> "unknown"
    }
}