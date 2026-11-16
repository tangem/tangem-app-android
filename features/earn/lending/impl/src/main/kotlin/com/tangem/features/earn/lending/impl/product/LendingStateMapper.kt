package com.tangem.features.earn.lending.impl.product

import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_lightning_20
import com.tangem.features.earn.lending.impl.provider.LendingDepositGate
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import com.tangem.features.earn.lending.impl.supplier.LendingAssetState
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnBlockAction
import com.tangem.features.earn.product.api.product.EarnLimits
import com.tangem.features.earn.product.api.product.EarnPosition
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.rate.formatEarnPercent
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.Withdraw
import java.math.BigDecimal

// Temporary plain text in this file: replace with localized string resources.
internal class LendingStateMapper(private val provider: LendingProvider) {

    private val performanceFeeText = provider.performanceFee.formatEarnPercent()

    private val details = listOf(
        EarnRow(
            label = resourceReference(R.string.earn_product_details_risk),
            value = EarnValue.Text(resourceReference(R.string.earn_risk_level_low)),
            info = EarnTextBlock(
                title = resourceReference(R.string.earn_product_details_risk_info_title),
                description = resourceReference(R.string.earn_product_details_risk_low_description),
            ),
        ),
        EarnRow(
            label = resourceReference(R.string.earn_product_details_protocol),
            value = EarnValue.Link(text = provider.info.title, url = provider.websiteUrl),
        ),
        infoRow(
            label = resourceReference(R.string.earn_product_details_withdrawals),
            value = EarnValue.Text(resourceReference(R.string.earn_product_details_usually_instant)),
            info = resourceReference(R.string.earn_product_details_lending_withdrawals_info),
        ),
        infoRow(
            label = resourceReference(R.string.earn_product_details_performance_fee),
            value = EarnValue.Text(
                resourceReference(R.string.earn_product_details_performance_fee_value, wrappedList(performanceFeeText)),
            ),
            info = resourceReference(
                R.string.earn_product_details_performance_fee_info,
                wrappedList(performanceFeeText),
            ),
        ),
    )

    private val updateRequiredBlock = EarnBlock(reason = LendingTexts.updateRequired(provider.info))

    fun map(state: LendingAssetState?): EarnProductState {
        val market = state?.market ?: return EarnProductState.Unavailable
        val rate = market.rate
        if (rate == null) {
            val isWaiting = market.deposit == LendingDepositGate.Loading
            return if (isWaiting) EarnProductState.Loading else EarnProductState.Unavailable
        }

        val position = market.position
        val symbol = state.asset.currency.symbol
        return EarnProductState.Ready(
            rate = rate,
            description = descriptionOf(symbol),
            projection = provider.projectionOf(rate.fraction),
            benefits = BENEFITS,
            details = details,
            // Any amount above zero is accepted: the screen checks it against the balance and the network fee
            limits = EarnLimits(minDeposit = BigDecimal.ZERO, remainingCap = null),
            apyPeriods = provider.apyPeriods,
            primary = primaryOf(state),
            secondary = if (position != null) listOf(withdrawOf(state)) else emptyList(),
            freshness = null,
            position = positionOf(state),
        )
    }

    private fun primaryOf(state: LendingAssetState): EarnActionItem = EarnActionItem(
        action = Deposit(),
        block = depositBlock(state),
        label = if (state.market.position != null) {
            resourceReference(R.string.common_add_funds)
        } else {
            stringReference("Start earning")
        },
    )

    private fun withdrawOf(state: LendingAssetState): EarnActionItem =
        EarnActionItem(action = WITHDRAW, block = withdrawBlock(state))

    private fun depositBlock(state: LendingAssetState): EarnBlock? = when {
        state.hasPendingTx -> PENDING_BLOCK
        state.market.deposit.isDepositBlocked -> updateRequiredBlock
        !state.market.isOpen -> CLOSED_BLOCK
        state.market.deposit == LendingDepositGate.ReadFailed -> RETRY_BLOCK
        else -> null
    }

    /** A closed market keeps withdrawals open, a provider that needs a newer app blocks them too */
    private fun withdrawBlock(state: LendingAssetState): EarnBlock? = when {
        state.hasPendingTx -> PENDING_BLOCK
        state.market.deposit.isDepositBlocked -> updateRequiredBlock
        else -> null
    }

    private fun positionOf(state: LendingAssetState): EarnPositionState {
        val position = state.market.position
        return when {
            position != null -> EarnPositionState.Known(
                EarnPosition(
                    cryptoAmount = position.cryptoAmount,
                    earned = null,
                    description = resourceReference(R.string.lending_position_about),
                    rows = listOf(
                        infoRow(
                            label = resourceReference(R.string.token_details_balance_total),
                            value = EarnValue.Amount(position.cryptoAmount),
                            info = resourceReference(
                                R.string.lending_position_total_balance_info,
                                wrappedList(performanceFeeText),
                            ),
                        ),
                        infoRow(
                            label = resourceReference(R.string.token_details_balance_available),
                            value = EarnValue.Amount(state.market.maxWithdraw ?: position.cryptoAmount),
                            info = resourceReference(R.string.lending_position_available_balance_info),
                        ),
                    ),
                    rewards = null,
                    freshness = null,
                ),
            )
            state.market.deposit == LendingDepositGate.ReadFailed -> EarnPositionState.Unknown
            else -> EarnPositionState.None
        }
    }

    private fun descriptionOf(symbol: String): TextReference = combinedReference(
        stringReference("Earn variable yield on $symbol by lending it directly on "),
        provider.info.title,
        stringReference(", "),
        provider.about,
        stringReference(". Withdrawals are usually instant."),
    )

    private companion object {
        val BENEFITS = listOf(
            EarnTextBlock(
                title = stringReference("No lock-ups."),
                description = stringReference("Send, swap, or sell your funds instantly, anytime you want"),
                icon = TangemIconUM.Icon(Icons.ic_lightning_20),
            ),
            EarnTextBlock(
                title = resourceReference(R.string.earn_product_details_lending_withdraw_anytime_title),
                description = resourceReference(R.string.earn_product_details_lending_withdraw_anytime_subtitle),
                icon = TangemIconUM.Icon(Icons.ic_arrow_refresh_20),
            ),
        )
        val WITHDRAW = Withdraw(notice = resourceReference(R.string.lending_withdraw_instant_notice))
        val PENDING_BLOCK = EarnBlock(reason = LendingTexts.pendingTransaction)
        val CLOSED_BLOCK = EarnBlock(reason = LendingTexts.depositsPaused)
        val RETRY_BLOCK = EarnBlock(
            reason = stringReference("Position is not loaded"),
            action = EarnBlockAction.Retry,
        )

        fun infoRow(label: TextReference, value: EarnValue, info: TextReference) = EarnRow(
            label = label,
            value = value,
            info = EarnTextBlock(title = label, description = info),
        )
    }
}