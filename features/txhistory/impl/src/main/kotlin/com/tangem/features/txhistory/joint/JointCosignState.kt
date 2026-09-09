package com.tangem.features.txhistory.joint

import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.models.account.Account

/**
 * A joint operation of the viewed transaction, decoded from its own `SafeTx`.
 *
 * @property displayed what the payload says is being sent; it is handed back to the sign use case, which refuses to
 * sign when the operation no longer matches what the screen showed
 * @property canSign whether this member may still answer
 * @property ownAnswer what this member already answered; `null` while they have not answered or their owner
 * address is not resolvable on this device
 */
internal data class JointCosignState(
    val account: Account.Joint,
    val displayed: JointTransferIntent,
    val threshold: Int,
    val membersCount: Int,
    val signers: List<JointOperationSignerState>,
    val canSign: Boolean,
    val ownAnswer: JointOperationSignerState.Answer?,
) {

    /** The initiator signed when creating the operation, so their answer counts towards the threshold */
    val collected: Int = signers.count { signer ->
        signer.answer == JointOperationSignerState.Answer.CREATED ||
            signer.answer == JointOperationSignerState.Answer.SIGNED
    }
}