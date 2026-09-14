package com.tangem.features.txhistory.joint.operation.model

import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigInteger

/**
 * @property displayed handed back to the sign use case, which refuses to sign an operation that no longer matches
 * what the screen showed
 * @property ownAnswer `null` while this member has not answered or their owner address is not resolvable here
 */
internal data class JointOperationState(
    val account: Account.Joint,
    val displayed: JointTransferIntent,
    val feeWei: BigInteger,
    val feeCoin: CryptoCurrency.Coin?,
    val threshold: Int,
    val membersCount: Int,
    val signers: List<JointOperationSignerState>,
    val canSign: Boolean,
    val ownAnswer: JointOperationSignerState.Answer?,
) {

    /** The initiator signed on creation, so their answer counts towards the threshold */
    val collected: Int = signers.count { signer ->
        signer.answer == JointOperationSignerState.Answer.CREATED ||
            signer.answer == JointOperationSignerState.Answer.SIGNED
    }
}