package com.tangem.features.earn.staking.impl

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.contributionOrNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.optionOrNull
import com.tangem.domain.staking.model.stakingBalanceData
import com.tangem.domain.staking.model.stakingSource
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRisk
import com.tangem.features.earn.product.api.product.ProductAvailability
import javax.inject.Inject

internal class PooledStakingMapper @Inject constructor() {

    fun summary(
        userWalletId: UserWalletId,
        status: CryptoCurrencyStatus,
        availability: StakingAvailability,
    ): EarnProductSummary? {
        val option = offerOf(status, availability) ?: return null
        val position = status.value.stakingBalanceData as? StakingBalance.Data.P2PEthPool

        return EarnProductSummary(
            productId = POOLED_STAKING_ID,
            rateText = stringReference("${option.apy.format { percent(canBeLower = true) }} APY"),
            risk = EarnRisk.Low,
            availability = if (availability is StakingAvailability.Full) {
                ProductAvailability.Full
            } else {
                ProductAvailability.Available
            },
            rate = EarnProductRate(fraction = option.apy, type = EarnRateType.APY),
            position = position?.let { EarnPositionSummary(cryptoAmount = it.totalStaked) },
            source = status.value.stakingSource,
            onClickAction = PooledStakingAction(buttonLabel(status.hasPossibleStake())),
            route = stakingRoute(userWalletId, status.currency),
        )
    }

    /** `Full` pools are offered only to users who already stake; other kinds of integration are not this product. */
    private fun offerOf(status: CryptoCurrencyStatus, availability: StakingAvailability): StakingOption.P2PEthPool? {
        val option = availability.optionOrNull as? StakingOption.P2PEthPool ?: return null
        val hasPosition = status.value.stakingBalanceData is StakingBalance.Data.P2PEthPool

        return option.takeUnless { availability is StakingAvailability.Full && !hasPosition }
    }

    /** A stake that is known or could not be read: "Start earning" must not be offered over a possible balance. */
    private fun CryptoCurrencyStatus.hasPossibleStake(): Boolean =
        value.stakingBalanceData is StakingBalance.Data.P2PEthPool ||
            value.contributionOrNull<StakingBalance.Error>() != null

    private fun buttonLabel(hasPossibleStake: Boolean): TextReference =
        stringReference(if (hasPossibleStake) MANAGE_LABEL else START_EARNING_LABEL)

    private companion object {

        const val START_EARNING_LABEL = "Start earning"
        const val MANAGE_LABEL = "Manage"
    }
}