package com.tangem.feature.tokendetails.presentation.tokendetails.state.utils

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.tokendetails.impl.R

internal fun Yield.Metadata.RewardSchedule.getStringResourceId(): Int {
    return when (this) {
        Yield.Metadata.RewardSchedule.BLOCK,
        Yield.Metadata.RewardSchedule.DAY,
        Yield.Metadata.RewardSchedule.ERA,
        Yield.Metadata.RewardSchedule.EPOCH,
        -> R.string.staking_notification_earn_rewards_text_daily

        Yield.Metadata.RewardSchedule.HOUR,
        -> R.string.staking_notification_earn_rewards_text_hourly

        Yield.Metadata.RewardSchedule.WEEK,
        -> R.string.staking_notification_earn_rewards_text_weekly

        Yield.Metadata.RewardSchedule.MONTH,
        -> R.string.staking_notification_earn_rewards_text_monthly

        else
        -> R.string.staking_notification_earn_rewards_text_daily
    }
}