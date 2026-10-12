package com.tangem.domain.jointaccount.model

import com.tangem.domain.models.account.Account

/**
 * @property account   the account the operation belongs to
 * @property transfer  what the payload says is being sent, decoded from the operation's `SafeTx`

 * in the roster the backend reported

 * not name them; `null` when the backend reported no initiator at all

 * @property ownAnswer what this member answered, [JointOperationSignerState.Answer.WAITING] while they have not.
 * `null` only when their owner address is not resolvable on this device or is absent from the signer list — not
 * answering is [JointOperationSignerState.Answer.WAITING], so a null check does not mean "has not answered"
 * @property collected answers that count towards the threshold, the initiator's included
 */
data class JointPendingOperation(
    val account: Account.Joint,
    val operationId: String,
    val transfer: JointOperationTransfer,
    val initiatorName: String?,
    val initiatorAddress: String?,
    val isOwn: Boolean,
    val ownAnswer: JointOperationSignerState.Answer?,
    val collected: Int,
    val threshold: Int,
)