package com.tangem.features.earn.lending.impl.product

import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
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
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.Withdraw
import java.math.BigDecimal

// Temporary plain text in this file: replace with localized string resources.
internal class LendingStateMapper(private val provider: LendingProvider) {

    private val details = listOf(
        textRow("Risk", "Low", info = "How risky the product is"),
        EarnRow(
            label = stringReference("Protocol"),
            value = EarnValue.Link(text = provider.info.title, url = provider.websiteUrl),
        ),
        textRow("Withdrawals", "Usually instant", info = "How fast you can withdraw"),
        textRow(
            label = "Performance fee",
            value = "${provider.performanceFee.format { percent(minFractionDigits = 0) }} of yield",
            info = "Taken from the earned yield",
        ),
    )

    private val updateRequiredBlock = EarnBlock(reason = LendingTexts.updateRequired(provider.info))

    /** @param minDeposit null when the provider could not tell: the limit is then not enforced by the screen */
    fun map(state: LendingAssetState?, minDeposit: BigDecimal?): EarnProductState {
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
            limits = EarnLimits(minDeposit = minDeposit ?: BigDecimal.ZERO, remainingCap = null),
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
        EarnActionItem(action = Withdraw(), block = withdrawBlock(state))

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
                    description = null,
                    rows = emptyList(),
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
                description = stringReference("Send, swap, or sell your funds anytime"),
                icon = TangemIconUM.Icon(Icons.ic_lightning_20),
            ),
            EarnTextBlock(
                title = stringReference("Withdraw anytime."),
                description = stringReference("Subject to protocol liquidity"),
                icon = TangemIconUM.Icon(Icons.ic_arrow_refresh_20),
            ),
        )
        val PENDING_BLOCK = EarnBlock(reason = LendingTexts.pendingTransaction)
        val CLOSED_BLOCK = EarnBlock(reason = LendingTexts.depositsPaused)
        val RETRY_BLOCK = EarnBlock(
            reason = stringReference("Position is not loaded"),
            action = EarnBlockAction.Retry,
        )

        fun textRow(label: String, value: String, info: String) = EarnRow(
            label = stringReference(label),
            value = EarnValue.Text(stringReference(value)),
            info = EarnTextBlock(stringReference(label), stringReference(info)),
        )
    }
}