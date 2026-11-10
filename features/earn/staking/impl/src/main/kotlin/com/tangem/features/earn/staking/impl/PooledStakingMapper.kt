package com.tangem.features.earn.staking.impl

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.contributionOrNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.P2PEthPoolIntegration
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
import com.tangem.features.earn.product.api.product.EarnTextBlock
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
            // Temporary plain text: becomes a resource taking the rate as an argument, and the APY suffix goes with it.
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
        if (status.value is CryptoCurrencyStatus.Loading) return EarnProductState.Loading

        val option = offerOf(status, availability) ?: return EarnProductState.Unavailable
        val position = status.value.stakingBalanceData as? StakingBalance.Data.P2PEthPool
        val bestVault = option.vaults.maxBy { it.apy }

        return EarnProductState.Ready(
            rate = EarnProductRate(fraction = option.apy, type = EarnRateType.APY),
            description = stringReference(DESCRIPTION),
            projection = DefaultEarnProjection(netRate = bestVault.netRate()),
            benefits = listOf(textBlock(BENEFIT_TITLE, BENEFIT_TEXT)),
            howItWorks = null,
            details = details(feePercent = bestVault.feePercent.stripTrailingZeros().toPlainString()),
            limits = EarnLimits(minDeposit = P2PEthPoolIntegration.DEFAULT_MINIMUM_STAKE, remainingCap = null),
            apyPeriods = emptyList(),
            primary = EarnActionItem(PooledStakingAction(buttonLabel(status.hasPossibleStake()))),
            freshness = null,
            position = positionOf(status, position),
        )
    }

    /**
     * `Full` pools are offered only to users who already stake; other kinds of integration are not this product.
     * A Loading or errored status carries no balances, so a stake cannot be told from none and nothing is offered.
     */
    private fun offerOf(status: CryptoCurrencyStatus, availability: StakingAvailability): StakingOption.P2PEthPool? {
        if (status.value is CryptoCurrencyStatus.Loading || status.value.isError) return null

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

    // Temporary plain text: the fee texts become resources taking the fee as an argument.
    private fun details(feePercent: String): List<EarnRow> = listOf(
        row(RISK_LABEL, RISK_VALUE),
        // The p2p.org link needs a link value in the contract; plain text until it exists.
        row(VALIDATOR_LABEL, VALIDATOR_VALUE),
        row(WITHDRAWALS_LABEL, WITHDRAWALS_VALUE, info = textBlock(WITHDRAWALS_LABEL, WITHDRAWALS_INFO)),
        row(
            label = FEE_LABEL,
            value = "$feePercent% of yield",
            info = textBlock(title = FEE_LABEL, description = "$feePercent$FEE_INFO_SUFFIX"),
        ),
    )

    private fun row(label: String, value: String, info: EarnTextBlock? = null): EarnRow = EarnRow(
        label = stringReference(label),
        value = EarnValue.Text(stringReference(value)),
        info = info,
    )

    private fun textBlock(title: String, description: String): EarnTextBlock = EarnTextBlock(
        title = stringReference(title),
        description = stringReference(description),
    )

    /** A stake that is known or could not be read: "Start earning" must not be offered over a possible balance. */
    private fun CryptoCurrencyStatus.hasPossibleStake(): Boolean =
        value.stakingBalanceData is StakingBalance.Data.P2PEthPool ||
            value.contributionOrNull<StakingBalance.Error>() != null

    private fun buttonLabel(hasPossibleStake: Boolean): TextReference =
        stringReference(if (hasPossibleStake) MANAGE_LABEL else START_EARNING_LABEL)

    private companion object {

        const val PERCENT_SHIFT = 2

        // Temporary plain strings: they become Lokalise resources in a separate strings task, and these constants go.
        const val START_EARNING_LABEL = "Start earning"
        const val MANAGE_LABEL = "Manage"
        const val DESCRIPTION = "Earn variable yield on ETH by staking with P2P, a professional validator that helps " +
            "secure Ethereum. Withdrawals take up to 5 days, and a small risk of loss applies if the validator is " +
            "penalised by the network."
        const val BENEFIT_TITLE = "Yield from Ethereum itself."
        const val BENEFIT_TEXT = "You earn from securing the network, not from lending."
        const val RISK_LABEL = "Risk"
        const val RISK_VALUE = "Low"
        const val VALIDATOR_LABEL = "Validator"
        const val VALIDATOR_VALUE = "P2P"
        const val WITHDRAWALS_LABEL = "Withdrawals"
        const val WITHDRAWALS_VALUE = "Up to 5 days"
        const val WITHDRAWALS_INFO = "Withdrawals take up to 5 days, because Ethereum releases staked ETH through an " +
            "exit queue."
        const val FEE_LABEL = "Performance fee"
        const val FEE_INFO_SUFFIX = "% of the yield you earn. It is not charged on the amount you deposit."
    }
}