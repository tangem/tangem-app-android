package com.tangem.features.staking.impl.presentation.state.utils

import com.tangem.core.ui.extensions.*
import com.tangem.domain.staking.model.common.RewardSchedule
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.utils.StakingRewardScheduleConstants.COSMOS_SCHEDULE
import com.tangem.features.staking.impl.presentation.state.utils.StakingRewardScheduleConstants.SOLANA_SCHEDULE
import com.tangem.features.staking.impl.presentation.state.utils.StakingRewardScheduleConstants.TON_SCHEDULE
import com.tangem.lib.crypto.BlockchainUtils.isCosmos
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.lib.crypto.BlockchainUtils.isTon
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.StringsSigns.MINUS
import com.tangem.utils.StringsSigns.NON_BREAKING_SPACE

private data object StakingRewardScheduleConstants {
    val COSMOS_SCHEDULE = 5 to 12
    val SOLANA_SCHEDULE = 2 to 3
    val TON_SCHEDULE = 1 to 2
}

internal fun getRewardScheduleText(
    rewardSchedule: RewardSchedule,
    networkId: String,
    decapitalize: Boolean,
): TextReference? {
    return when (rewardSchedule) {
        RewardSchedule.WEEK -> resourceReference(
            id = R.string.staking_reward_schedule_week,
            decapitalize = decapitalize,
        )
        RewardSchedule.HOUR -> resourceReference(
            id = R.string.staking_reward_schedule_hour,
            decapitalize = decapitalize,
        )
        RewardSchedule.DAY -> resourceReference(
            id = R.string.staking_reward_schedule_day,
            decapitalize = decapitalize,
        )
        RewardSchedule.MONTH -> resourceReference(
            id = R.string.staking_reward_schedule_month,
            decapitalize = decapitalize,
        )
        RewardSchedule.BLOCK,
        RewardSchedule.EPOCH,
        RewardSchedule.ERA,
        -> getCustomRewardSchedule(
            networkId = networkId,
            decapitalize = decapitalize,
        )
        RewardSchedule.UNKNOWN -> null
    }
}

internal fun getRewardTypeShortText(rewardType: RewardType): TextReference {
    return when (rewardType) {
        RewardType.APR -> TextReference.Res(R.string.staking_details_apr)
        RewardType.APY -> TextReference.Res(R.string.staking_details_apy)
        else -> TextReference.EMPTY
    }
}

internal fun getRewardTypeLongText(rewardType: RewardType): TextReference {
    return when (rewardType) {
        RewardType.APR -> TextReference.Res(R.string.staking_details_annual_percentage_rate)
        RewardType.APY -> TextReference.Res(R.string.staking_details_annual_percentage_yield)
        else -> TextReference.EMPTY
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
        isTon(networkId) -> combinedReference(
            resourceReference(id = R.string.staking_reward_schedule_each_plural, decapitalize = decapitalize),
            stringReference(NON_BREAKING_SPACE.toString()),
            stringReference("${TON_SCHEDULE.first}$MINUS${TON_SCHEDULE.second}$NON_BREAKING_SPACE"),
            pluralReference(
                id = R.plurals.common_days_no_param,
                count = TON_SCHEDULE.second,
            ),
        )
        else -> null
    }
}