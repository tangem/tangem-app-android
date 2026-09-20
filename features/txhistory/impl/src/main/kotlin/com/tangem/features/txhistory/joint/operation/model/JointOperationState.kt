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

 * report one
 * @property isSending  the threshold is reached and the operation is on its way to the network; nothing to sign or
 * refuse any more
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
    val initiator: String?,
    val isSending: Boolean = false,
) {

    /**
     * The initiator deletes the operation, every other member declines it. The backend tells them apart by the
     * caller, so a member whose role this screen cannot tell is offered neither action.
     */
    val role: Role = when {
        ownAddress == null || initiator == null -> Role.UNKNOWN
        ownAddress.equals(initiator, ignoreCase = true) -> Role.INITIATOR
        else -> Role.MEMBER
    }

    enum class Role { INITIATOR, MEMBER, UNKNOWN }

    /** The initiator signed on creation, so their answer counts towards the threshold */
    val collected: Int = signers.count { signer ->
        signer.answer == JointOperationSignerState.Answer.CREATED ||
            signer.answer == JointOperationSignerState.Answer.SIGNED
    }

    /** Every member who has not declined can still sign, so the threshold is out of reach once too many declined */
    val isReachable: Boolean =
        membersCount - signers.count { it.answer == JointOperationSignerState.Answer.DECLINED } >= threshold

    private val ownSigner: JointOperationSignerState? = signers.firstOrNull { signer ->
        ownAddress != null && signer.member.address.equals(ownAddress, ignoreCase = true)
    }

    /** `null` while this member has not answered or their owner address is not resolvable here */
    val ownAnswer: JointOperationSignerState.Answer? = ownSigner?.answer

    /**
     * The backend answers a caller outside the roster with `NotAMember`, and [signers] carries every member of the
     * account, so a missing [ownAnswer] also means this owner address is not one of them.
     */
    val canRefuse: Boolean = role != Role.UNKNOWN &&
        ownAnswer != null &&
        ownAnswer != JointOperationSignerState.Answer.DECLINED

    /**
     * The sign and reject use cases tolerate a failed refresh, so [signers] can still carry this member as waiting
     * after they answered. [answeredLocally] holds that answer until a refresh lands, so every reader of this state —
     * the signers list and [collected] alike — sees the same answer.
     */
    fun withOwnAnswer(answeredLocally: JointOperationSignerState.Answer?): JointOperationState {
        val isWaiting = ownSigner?.answer == JointOperationSignerState.Answer.WAITING
        if (answeredLocally == null || !isWaiting) return this

        return copy(
            signers = signers.map { signer ->
                if (signer == ownSigner) signer.copy(answer = answeredLocally) else signer
            },
        )
    }
}