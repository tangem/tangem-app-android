package com.tangem.features.earn.staking.impl

import com.tangem.common.routing.AppRoute
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.optionOrNull
import com.tangem.domain.staking.model.stakingBalanceData
import com.tangem.domain.staking.model.stakingSource
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.product.EarnRateEntry
import javax.inject.Inject

internal class StakingRateMapper @Inject constructor() {

    fun entry(
        userWalletId: UserWalletId,
        status: CryptoCurrencyStatus,
        availability: StakingAvailability,
    ): EarnRateEntry? {
        val option = availability.optionOrNull as? StakingOption.StakeKit ?: return null
        val balance = status.value.stakingBalanceData as? StakingBalance.Data.StakeKit
        val isActive = balance != null

        if (availability is StakingAvailability.Full && !isActive) return null

        val reward = option.stakedValidatorReward(balance) ?: option.preferredValidatorReward() ?: return null

        return EarnRateEntry(
            rate = EarnProductRate(fraction = reward.rate, type = reward.type.toEarnRateType()),
            isActive = isActive,
            source = status.value.stakingSource,
            route = AppRoute.Staking(userWalletId, status.currency, option.integrationId),
        )
    }

    private fun StakingOption.StakeKit.stakedValidatorReward(balance: StakingBalance.Data.StakeKit?): RewardInfo? {
        val validatorsByAddress = yield.validators.associateBy { it.address }
        return balance?.balance
            ?.items
            ?.mapNotNull { it.validatorAddress }
            ?.firstNotNullOfOrNull { address -> validatorsByAddress[address]?.rewardInfo }
    }

    private fun StakingOption.StakeKit.preferredValidatorReward(): RewardInfo? = yield.validators
        .filter { it.isPreferred }
        .mapNotNull { it.rewardInfo }
        .maxByOrNull { it.rate }

    private fun RewardType.toEarnRateType(): EarnRateType = when (this) {
        RewardType.APR -> EarnRateType.APR
        RewardType.APY, RewardType.UNKNOWN -> EarnRateType.APY
    }
}