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

internal fun getRewardScheduleText(
    rewardSchedule: Yield.Metadata.RewardSchedule,
    networkId: String,
    decapitalize: Boolean,
): TextReference? {
    return when (rewardSchedule) {
        Yield.Metadata.RewardSchedule.WEEK -> resourceReference(
            id = R.string.staking_reward_schedule_week,
            decapitalize = decapitalize,
        )
        Yield.Metadata.RewardSchedule.HOUR -> resourceReference(
            id = R.string.staking_reward_schedule_hour,
            decapitalize = decapitalize,
        )
        Yield.Metadata.RewardSchedule.DAY -> resourceReference(
            id = R.string.staking_reward_schedule_day,
            decapitalize = decapitalize,
        )
        Yield.Metadata.RewardSchedule.MONTH -> resourceReference(
            id = R.string.staking_reward_schedule_month,
            decapitalize = decapitalize,
        )
        Yield.Metadata.RewardSchedule.BLOCK,
        Yield.Metadata.RewardSchedule.EPOCH,
        Yield.Metadata.RewardSchedule.ERA,
        -> getCustomRewardSchedule(
            networkId = networkId,
            decapitalize = decapitalize,
        )
        else -> null
    }
}

private fun getCustomRewardSchedule(networkId: String, decapitalize: Boolean = false): TextReference? {
    return when {
        isSolana(networkId) -> {
            combinedReference(
                resourceReference(id = R.string.staking_reward_schedule_each_plural, decapitalize = decapitalize),
                stringReference(NON_BREAKING_SPACE.toString()),
                stringReference("${SOLANA_SCHEDULE.first}$MINUS${SOLANA_SCHEDULE.second}$NON_BREAKING_SPACE"),
                pluralReference(
                    id = R.plurals.common_days_no_param,
                    count = SOLANA_SCHEDULE.second,
                ),
            )
        }
        isCosmos(networkId) -> {
            combinedReference(
                resourceReference(id = R.string.staking_reward_schedule_each_plural, decapitalize = decapitalize),
                stringReference(NON_BREAKING_SPACE.toString()),
                stringReference("${COSMOS_SCHEDULE.first}$MINUS${COSMOS_SCHEDULE.second}$NON_BREAKING_SPACE"),
                resourceReference(R.string.common_second_no_param),
            )
        }
        isTron(networkId) -> resourceReference(id = R.string.staking_reward_schedule_day, decapitalize = decapitalize)
        else -> null
    }
}