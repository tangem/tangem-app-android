package com.tangem.domain.staking.model.stakekit

import com.tangem.domain.models.StatusSource
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import org.joda.time.DateTime
import java.math.BigDecimal

sealed class YieldBalance {

    abstract val integrationId: String?
    abstract val address: String?

    data class Data(
        override val integrationId: String?,
        override val address: String,
        val balance: YieldBalanceItem,
        val source: StatusSource,
    ) : YieldBalance() {
        fun getTotalWithRewardsStakingBalance(): BigDecimal {
            return balance.items.sumOf { it.amount }
        }

        fun getTotalStakingBalance(): BigDecimal {
            return balance.items
                .filterNot { it.type == BalanceType.REWARDS }
                .sumOf { it.amount }
        }

        fun getRewardStakingBalance(): BigDecimal {
            return balance.items
                .filter { it.type == BalanceType.REWARDS }
                .sumOf { it.amount }
        }

        fun getValidatorsCount(): Int {
            return balance.items
                .filterNot { it.validatorAddress.isNullOrBlank() }
                .distinctBy { it.validatorAddress }
                .size
        }
    }

    data class Empty(
        override val integrationId: String?,
        override val address: String,
        val source: StatusSource,
    ) : YieldBalance()

    data class Error(override val integrationId: String?, override val address: String?) : YieldBalance()
}

data class YieldBalanceItem(
    val items: List<BalanceItem>,
    val integrationId: String?,
)

data class BalanceItem(
    val groupId: String,
    val token: Token,
    val type: BalanceType,
    val amount: BigDecimal,
    val rawCurrencyId: String?,
    val validatorAddress: String?,
    val date: DateTime?,
    val pendingActions: List<PendingAction>,
    val isPending: Boolean,
)

data class PendingAction(
    val type: StakingActionType,
    val passthrough: String,
    val args: PendingActionArgs?,
) {
    data class PendingActionArgs(
        val amount: Amount?,
        val duration: Duration?,
        val validatorAddress: Boolean?,
        val validatorAddresses: Boolean?,
        val tronResource: TronResource?,
        val signatureVerification: Boolean?,
    ) {
        data class Amount(
            val required: Boolean,
            val minimum: BigDecimal?,
            val maximum: BigDecimal?,
        )

        data class Duration(
            val required: Boolean,
            val minimum: Int?,
            val maximum: Int?,
        )

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

enum class RewardBlockType {
    NoRewards,
    Rewards,
    RewardUnavailable,
}