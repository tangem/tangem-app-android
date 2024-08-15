package com.tangem.domain.staking.model.stakekit

import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import java.math.BigDecimal

sealed class YieldBalance {

    data class Data(
        val balance: YieldBalanceItem,
    ) : YieldBalance() {
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
    }

    data object Empty : YieldBalance()

    data object Error : YieldBalance()
}

data class YieldBalanceItem(
    val items: List<BalanceItem>,
    val integrationId: String?,
)

data class BalanceItem(
    val type: BalanceType,
    val amount: BigDecimal,
    val pricePerShare: BigDecimal,
    val rawCurrencyId: String?,
    val rawNetworkId: String,
    val validatorAddress: String?,
    val pendingActions: List<PendingAction>,
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

enum class BalanceType {
    AVAILABLE,
    STAKED,
    UNSTAKING,
    UNSTAKED,
    PREPARING,
    REWARDS,
    LOCKED,
    UNLOCKING,
    UNKNOWN,
}
