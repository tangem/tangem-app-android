package com.tangem.domain.jointaccount.safe

import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperation.MemberSignature.State
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer

/**
 * Joins the account roster with the operation's signature records: the backend only lists the members who answered,
 * the rest of [JointAccount.members] are waiting. Addresses are compared case-insensitively.
 */
class JointOperationSignersResolver {

    fun signers(jointAccount: JointAccount, operation: JointOperation): List<JointOperationSignerState> {
        return jointAccount.members.map { member ->
            val signature = operation.signatures.firstOrNull { it.address.equals(member.address, ignoreCase = true) }
            val answer = when {
                member.address.equals(operation.initiator, ignoreCase = true) -> Answer.CREATED
                signature?.state == State.SIGNED -> Answer.SIGNED
                signature?.state == State.DECLINED -> Answer.DECLINED
                else -> Answer.WAITING
            }

            // The initiator signs by creating the operation, so its creation time is their answer time
            val at = when (answer) {
                Answer.CREATED -> signature?.at ?: operation.createdAt
                Answer.WAITING -> null
                Answer.SIGNED, Answer.DECLINED -> signature?.at
            }
            JointOperationSignerState(member = member, answer = answer, at = at)
        }
    }

    /** Sign is offered once per member of [jointAccount] and only while the operation still collects signatures */
    fun canSign(jointAccount: JointAccount, operation: JointOperation, ownerAddress: String?): Boolean {
        if (ownerAddress == null) return false
        // An address outside the roster is refused by the verifier as NOT_A_MEMBER, so it must not be offered
        if (jointAccount.members.none { it.address.equals(ownerAddress, ignoreCase = true) }) return false
        if (operation.status != JointOperation.Status.AWAITING_SIGNATURES) return false
        if (ownerAddress.equals(operation.initiator, ignoreCase = true)) return false

        return operation.signatures.none { it.address.equals(ownerAddress, ignoreCase = true) }
    }
}