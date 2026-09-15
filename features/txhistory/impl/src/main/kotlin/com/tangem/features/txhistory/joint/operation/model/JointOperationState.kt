package com.tangem.features.txhistory.joint.operation.model

import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigInteger

/**
 * @property displayed handed back to the sign use case, which refuses to sign an operation that no longer matches
 * what the screen showed
 * @property ownAddress this member's owner address, `null` where it is not resolvable here
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
    val ownAddress: String?,
) {

    /** The initiator signed on creation, so their answer counts towards the threshold */
    val collected: Int = signers.count { signer ->
        signer.answer == JointOperationSignerState.Answer.CREATED ||
            signer.answer == JointOperationSignerState.Answer.SIGNED
    }

    private val ownSigner: JointOperationSignerState? = signers.firstOrNull { signer ->
        ownAddress != null && signer.member.address.equals(ownAddress, ignoreCase = true)
    }

    /** `null` while this member has not answered or their owner address is not resolvable here */
    val ownAnswer: JointOperationSignerState.Answer? = ownSigner?.answer

    /**
     * The sign use case tolerates a failed refresh, so [signers] can still carry this member as waiting after they
     * signed. [answeredLocally] holds that answer until a refresh lands, so every reader of this state — the signers
     * list and [collected] alike — sees the same answer.
     */
    fun withOwnAnswer(answeredLocally: Boolean): JointOperationState {
        val isWaiting = ownSigner?.answer == JointOperationSignerState.Answer.WAITING
        if (!answeredLocally || !isWaiting) return this

        return copy(
            signers = signers.map { signer ->
                if (signer == ownSigner) {
                    signer.copy(answer = JointOperationSignerState.Answer.SIGNED)
                } else {
                    signer
                }
            },
        )
    }
}