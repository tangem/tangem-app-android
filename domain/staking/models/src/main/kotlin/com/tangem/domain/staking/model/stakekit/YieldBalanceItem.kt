package com.tangem.domain.staking.model.stakekit

import com.tangem.domain.core.serialization.SerializedBigDecimal
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class YieldBalanceItem(
    val items: List<BalanceItem>,
    val integrationId: String,
)

@Serializable
data class BalanceItem(
    val groupId: String,
    val token: Token,
    val type: BalanceType,
    val amount: SerializedBigDecimal,
    val rawCurrencyId: String?,
    val validatorAddress: String?,
    val date: Instant?,
    val pendingActions: List<PendingAction>,
    val pendingActionsConstraints: List<PendingActionConstraints>,
    val isPending: Boolean,
)

@Serializable
data class PendingActionConstraints(
    val type: StakingActionType,
    val amountArg: PendingAction.PendingActionArgs.Amount?,
)

@Serializable
data class PendingAction(
    val type: StakingActionType,
    val passthrough: String,
    val args: PendingActionArgs?,
) {

    @Serializable
    data class PendingActionArgs(
        val amount: Amount?,
        val duration: Duration?,
        val validatorAddress: Boolean?,
        val validatorAddresses: Boolean?,
        val tronResource: TronResource?,
        val signatureVerification: Boolean?,
    ) {

        @Serializable
        data class Amount(
            val required: Boolean,
            val minimum: SerializedBigDecimal?,
            val maximum: SerializedBigDecimal?,
        )

        @Serializable
        data class Duration(
            val required: Boolean,
            val minimum: Int?,
            val maximum: Int?,
        )

        @Serializable
        data class TronResource(
            val required: Boolean,
            val options: List<String>,
        )
    }
}

/**
 * IMPORTANT!!!
 * Order is used to sort balances.
 */
@Serializable
@Suppress("MagicNumber")
enum class BalanceType(val order: Int) {
    AVAILABLE(1),
    STAKED(2),
    PREPARING(3),
    LOCKED(4),
    UNSTAKING(5),
    UNLOCKING(6),
    UNSTAKED(7),
    REWARDS(8),
    UNKNOWN(9),
    ;

    companion object {

        fun BalanceType.isClickable() = when (this) {
            STAKED,
            UNSTAKED,
            LOCKED,
            -> true
            AVAILABLE,
            UNSTAKING,
            PREPARING,
            REWARDS,
            UNLOCKING,
            UNKNOWN,
            -> false
        }
    }
}

@Serializable
enum class RewardBlockType {
    NoRewards,
    Rewards,
    RewardsRequirementsError,
    RewardUnavailable,
    ;

    /**
     * Indicated whether action can be performed on available reward
     * For example, pending action CLAIM_REWARDS could be called
     */
    val isActionable: Boolean
        get() = when (this) {
            NoRewards,
            RewardUnavailable,
            -> false
            RewardsRequirementsError,
            Rewards,
            -> true
        }
}