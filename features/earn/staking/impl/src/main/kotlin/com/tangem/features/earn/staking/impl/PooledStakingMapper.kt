package com.tangem.features.earn.staking.impl

import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
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
import com.tangem.core.ui.R as CoreUiR

internal class PooledStakingMapper @Inject constructor() {

    fun summary(
        userWalletId: UserWalletId,
        status: CryptoCurrencyStatus,
        availability: StakingAvailability,
        isBalanceFailed: Boolean = false,
    ): EarnProductSummary? {
        val option = offerOf(status, availability) ?: return null
        val position = status.value.stakingBalanceData as? StakingBalance.Data.P2PEthPool

        return EarnProductSummary(
            productId = POOLED_STAKING_ID,
            rateText = resourceReference(
                id = R.string.earn_apy_value,
                formatArgs = wrappedList(option.apy.format { percent(canBeLower = true) }),
            ),
            risk = EarnRisk.Low,
            availability = if (availability is StakingAvailability.Full) {
                ProductAvailability.Full
            } else {
                ProductAvailability.Available
            },
            rate = EarnProductRate(fraction = option.apy, type = EarnRateType.APY),
            position = position?.let { EarnPositionSummary(cryptoAmount = it.totalStaked) },
            source = status.value.stakingSource,
            onClickAction = PooledStakingAction(buttonLabel(hasPossibleStake(position, isBalanceFailed))),
            route = stakingRoute(userWalletId, status.currency),
        )
    }

    fun state(
        status: CryptoCurrencyStatus,
        availability: StakingAvailability,
        isBalanceFailed: Boolean = false,
    ): EarnProductState {
        if (status.value is CryptoCurrencyStatus.Loading) return EarnProductState.Loading

        val option = offerOf(status, availability) ?: return EarnProductState.Unavailable
        val position = status.value.stakingBalanceData as? StakingBalance.Data.P2PEthPool
        val bestVault = option.vaults.maxBy { it.apy }

        return EarnProductState.Ready(
            rate = EarnProductRate(fraction = option.apy, type = EarnRateType.APY),
            description = resourceReference(R.string.earn_yield_picker_pooled_staking_description),
            projection = DefaultEarnProjection(netRate = bestVault.netRate()),
            benefits = listOf(
                EarnTextBlock(
                    title = resourceReference(R.string.earn_product_details_staking_benefit_title),
                    description = resourceReference(R.string.earn_product_details_staking_benefit_subtitle),
                    icon = TangemIconUM.Icon(CoreUiR.drawable.ic_staking_24),
                ),
            ),
            details = details(feePercent = bestVault.feePercent.stripTrailingZeros().toPlainString()),
            limits = EarnLimits(minDeposit = P2PEthPoolIntegration.DEFAULT_MINIMUM_STAKE, remainingCap = null),
            apyPeriods = emptyList(),
            primary = EarnActionItem(PooledStakingAction(buttonLabel(hasPossibleStake(position, isBalanceFailed)))),
            freshness = null,
            position = positionOf(position, isBalanceFailed),
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

    private fun positionOf(data: StakingBalance.Data.P2PEthPool?, isBalanceFailed: Boolean): EarnPositionState = when {
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
        isBalanceFailed -> EarnPositionState.Unknown
        else -> EarnPositionState.None
    }

    private fun details(feePercent: String): List<EarnRow> {
        val percent = "$feePercent%"
        val withdrawalsLabel = resourceReference(R.string.earn_product_details_withdrawals)
        val feeLabel = resourceReference(R.string.earn_product_details_performance_fee)

        return listOf(
            row(
                label = resourceReference(R.string.earn_product_details_risk),
                value = resourceReference(R.string.earn_risk_level_low),
            ),
            // The p2p.org link needs a link value in the contract; plain text until it exists.
            row(label = resourceReference(R.string.staking_validator), value = stringReference(VALIDATOR_VALUE)),
            row(
                label = withdrawalsLabel,
                value = resourceReference(R.string.earn_product_details_staking_withdrawals_value),
                info = textBlock(
                    title = withdrawalsLabel,
                    description = resourceReference(R.string.earn_product_details_staking_withdrawals_info),
                ),
            ),
            row(
                label = feeLabel,
                value = resourceReference(R.string.earn_product_details_performance_fee_value, wrappedList(percent)),
                info = textBlock(
                    title = feeLabel,
                    description = resourceReference(
                        id = R.string.earn_product_details_performance_fee_info,
                        formatArgs = wrappedList(percent),
                    ),
                ),
            ),
        )
    }

    private fun row(label: TextReference, value: TextReference, info: EarnTextBlock? = null): EarnRow = EarnRow(
        label = label,
        value = EarnValue.Text(value),
        info = info,
    )

    private fun textBlock(title: TextReference, description: TextReference): EarnTextBlock = EarnTextBlock(
        title = title,
        description = description,
    )

    /** A stake that is known or could not be read: "Start earning" must not be offered over a possible balance. */
    private fun hasPossibleStake(position: StakingBalance.Data.P2PEthPool?, isBalanceFailed: Boolean): Boolean =
        position != null || isBalanceFailed

    private fun buttonLabel(hasPossibleStake: Boolean): TextReference = if (hasPossibleStake) {
        resourceReference(R.string.eth_yield_manage)
    } else {
        resourceReference(R.string.eth_yield_start_earning)
    }

    private companion object {

        const val PERCENT_SHIFT = 2

        // The validator is a brand name, not a translated text.
        const val VALIDATOR_VALUE = "P2P"
    }
}