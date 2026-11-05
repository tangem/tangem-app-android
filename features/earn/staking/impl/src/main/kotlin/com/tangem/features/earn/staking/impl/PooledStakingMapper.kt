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
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.optionOrNull
import com.tangem.domain.staking.model.stakingBalanceData
import com.tangem.domain.staking.model.stakingSource
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.product.DefaultEarnProjection
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnLimits
import com.tangem.features.earn.product.api.product.EarnPosition
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRisk
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.product.ProductAvailability
import java.math.BigDecimal
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

    fun state(status: CryptoCurrencyStatus, availability: StakingAvailability): EarnProductState {
        val option = offerOf(status, availability) ?: return EarnProductState.Unavailable
        val position = status.value.stakingBalanceData as? StakingBalance.Data.P2PEthPool
        val bestVault = option.vaults.maxBy { it.apy }

        return EarnProductState.Ready(
            rate = EarnProductRate(fraction = option.apy, type = EarnRateType.APY),
            description = stringReference(DESCRIPTION),
            projection = DefaultEarnProjection(netRate = bestVault.netRate()),
            benefits = emptyList(),
            howItWorks = null,
            details = listOf(
                row(RISK_LABEL, RISK_VALUE),
                row(PROTOCOL_LABEL, PROTOCOL_VALUE),
                row(FEE_LABEL, "${bestVault.feePercent.stripTrailingZeros().toPlainString()}%"),
            ),
            limits = EarnLimits(minDeposit = BigDecimal.ZERO, remainingCap = null),
            apyPeriods = emptyList(),
            primary = EarnActionItem(PooledStakingAction(buttonLabel(status.hasPossibleStake()))),
            freshness = null,
            position = positionOf(status, position),
        )
    }

    /** `Full` pools are offered only to users who already stake; other kinds of integration are not this product. */
    private fun offerOf(status: CryptoCurrencyStatus, availability: StakingAvailability): StakingOption.P2PEthPool? {
        val option = availability.optionOrNull as? StakingOption.P2PEthPool ?: return null
        val hasPosition = status.value.stakingBalanceData is StakingBalance.Data.P2PEthPool

        return option.takeUnless { availability is StakingAvailability.Full && !hasPosition }
    }

    /** The rate the app shows is gross: the performance fee of the same vault is taken off for estimates (D7). */
    private fun P2PEthPoolVault.netRate(): BigDecimal = apy * (BigDecimal.ONE - feePercent.movePointLeft(PERCENT_SHIFT))

    private fun positionOf(status: CryptoCurrencyStatus, data: StakingBalance.Data.P2PEthPool?): EarnPositionState =
        when {
            data != null -> EarnPositionState.Known(
                EarnPosition(
                    cryptoAmount = data.totalStaked,
                    earned = data.totalRewards,
                    description = null,
                    rows = emptyList(),
                    rewards = null,
                    freshness = null,
                ),
            )
            status.value.contributionOrNull<StakingBalance.Error>() != null -> EarnPositionState.Unknown
            else -> EarnPositionState.None
        }

    private fun row(label: String, value: String): EarnRow = EarnRow(
        label = stringReference(label),
        value = EarnValue.Text(stringReference(value)),
    )

    /** A stake that is known or could not be read: "Start earning" must not be offered over a possible balance. */
    private fun CryptoCurrencyStatus.hasPossibleStake(): Boolean =
        value.stakingBalanceData is StakingBalance.Data.P2PEthPool ||
            value.contributionOrNull<StakingBalance.Error>() != null

    private fun buttonLabel(hasPossibleStake: Boolean): TextReference =
        stringReference(if (hasPossibleStake) MANAGE_LABEL else START_EARNING_LABEL)

    private companion object {

        const val PERCENT_SHIFT = 2
        const val START_EARNING_LABEL = "Start earning"
        const val MANAGE_LABEL = "Manage"
        const val DESCRIPTION = "Earn rewards by staking ETH in a P2P.org pool"
        const val RISK_LABEL = "Risk"
        const val RISK_VALUE = "Low"
        const val PROTOCOL_LABEL = "Protocol"
        const val PROTOCOL_VALUE = "P2P.org"
        const val FEE_LABEL = "Performance fee"
    }
}