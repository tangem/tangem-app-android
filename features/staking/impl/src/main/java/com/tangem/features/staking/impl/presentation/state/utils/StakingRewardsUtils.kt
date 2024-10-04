package com.tangem.features.staking.impl.presentation.state.utils

import com.tangem.core.ui.extensions.*
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.utils.StakingRewardSchedule.COSMOS_SCHEDULE
import com.tangem.features.staking.impl.presentation.state.utils.StakingRewardSchedule.SOLANA_SCHEDULE
import com.tangem.lib.crypto.BlockchainUtils.isCosmos
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.StringsSigns.MINUS
import com.tangem.utils.StringsSigns.NON_BREAKING_SPACE

private data object StakingRewardSchedule {
    val COSMOS_SCHEDULE = 5 to 12
    val SOLANA_SCHEDULE = 2 to 3
}

internal fun getRewardSchedule(schedule: Yield.Metadata.RewardSchedule, networkId: String): TextReference? =
    when (schedule) {
        Yield.Metadata.RewardSchedule.BLOCK,
        Yield.Metadata.RewardSchedule.EPOCH,
        Yield.Metadata.RewardSchedule.ERA,
        -> getCustomRewardSchedule(networkId)
        Yield.Metadata.RewardSchedule.WEEK -> resourceReference(R.string.common_week)
        Yield.Metadata.RewardSchedule.HOUR -> resourceReference(R.string.common_hour)
        Yield.Metadata.RewardSchedule.DAY -> pluralReference(R.plurals.common_days_no_param, count = 1)
        Yield.Metadata.RewardSchedule.MONTH -> resourceReference(R.string.common_month)
        Yield.Metadata.RewardSchedule.UNKNOWN -> null
    }

private fun getCustomRewardSchedule(networkId: String): TextReference? {
    return when {
        isSolana(networkId) -> {
            combinedReference(
                stringReference("${SOLANA_SCHEDULE.first}$MINUS${SOLANA_SCHEDULE.second}$NON_BREAKING_SPACE"),
                pluralReference(
                    id = R.plurals.common_days_no_param,
                    count = SOLANA_SCHEDULE.second,
                ),
            )
        }
        isCosmos(networkId) -> {
            combinedReference(
                stringReference("${COSMOS_SCHEDULE.first}$MINUS${COSMOS_SCHEDULE.second}$NON_BREAKING_SPACE"),
                resourceReference(R.string.common_second_no_param),
            )
        }
        isTron(networkId) -> pluralReference(R.plurals.common_days_no_param, count = 1)
        else -> null
    }
}
