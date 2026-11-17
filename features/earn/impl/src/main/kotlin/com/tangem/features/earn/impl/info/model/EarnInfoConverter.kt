package com.tangem.features.earn.impl.info.model

import com.tangem.common.ui.amountScreen.utils.getFiatString
import com.tangem.core.res.R
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.earn.impl.guardedBy
import com.tangem.features.earn.impl.info.EarnInfoActions
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab
import com.tangem.features.earn.impl.info.ui.state.EarnInfoUM
import com.tangem.features.earn.impl.picker.ui.state.EarnBenefitUM
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnRewards
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.toUM
import com.tangem.features.earn.product.api.session.EarnAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

internal class EarnInfoConverter @Inject constructor(
    private val actions: EarnInfoActions,
) {

    @Suppress("LongParameterList")
    fun convert(
        product: EarnProduct,
        status: CryptoCurrencyStatus,
        appCurrency: AppCurrency,
        isBalanceHidden: Boolean,
        ready: EarnProductState.Ready,
        callbacks: Callbacks,
    ): EarnInfoUM? {
        val position = (ready.position as? EarnPositionState.Known)?.position ?: return null
        val rate = status.value.fiatRate
        val isStale = listOfNotNull(ready.freshness, position.freshness).any { it.isStale }

        return EarnInfoUM(
            title = product.title,
            subtitle = resourceReference(R.string.lending_position_subtitle),
            balanceFiat = stringReference(
                getFiatString(position.cryptoAmount, rate, appCurrency).orMaskWithStars(isBalanceHidden),
            ),
            balanceCrypto = cryptoText(position.cryptoAmount, status.currency, isBalanceHidden),
            rewards = position.rewards?.let { rewards ->
                rewardsBanner(
                    rewards = rewards,
                    amount = getFiatString(rewards.amount, rate, appCurrency).orMaskWithStars(isBalanceHidden),
                    isStale = isStale,
                    callbacks = callbacks,
                )
            },
            tabs = tabs(ready),
            myFunds = EarnInfoUM.MyFundsUM(
                provider = product.provider.title,
                providerIcon = product.provider.icon,
                description = position.description,
                rows = position.rows.map { row ->
                    row.toUM(callbacks.onInfoClick, callbacks.onLinkClick) { amount ->
                        cryptoText(amount, status.currency, isBalanceHidden)
                    }
                }.toImmutableList(),
                onHowItWorksClick = product.howItWorks
                    ?.takeIf { it.entries.isNotEmpty() }
                    ?.let { { callbacks.onHowItWorksClick(it) } },
            ),
            benefits = ready.benefits.map { it.toBenefitUM() }.toImmutableList(),
            details = ready.details.map { row ->
                row.toUM(callbacks.onInfoClick, callbacks.onLinkClick) { amount ->
                    cryptoText(amount, status.currency, isBalanceHidden = false)
                }
            }.toImmutableList(),
            secondary = ready.secondary.map { item ->
                button(item = item, type = TangemButtonType.Secondary, isStale = isStale, callbacks = callbacks)
            }.toImmutableList(),
            primary = button(
                item = ready.primary,
                type = TangemButtonType.Primary,
                isStale = isStale,
                callbacks = callbacks,
            ),
            onClose = callbacks.onClose,
        )
    }

    private fun button(item: EarnActionItem, type: TangemButtonType, isStale: Boolean, callbacks: Callbacks) =
        actions.button(
            item = item.guardedBy(isStale = isStale),
            type = type,
            onActionClick = callbacks.onActionClick,
            onBlocked = callbacks.onBlocked,
        )

    private fun rewardsBanner(rewards: EarnRewards, amount: String, isStale: Boolean, callbacks: Callbacks) =
        EarnInfoUM.RewardsUM(
            title = resourceReference(R.string.yield_module_rewards_banner_title, wrappedList(amount)),
            description = resourceReference(R.string.yield_module_rewards_banner_subtitle),
            claim = button(
                item = EarnActionItem(rewards.claimAction),
                type = TangemButtonType.Primary,
                isStale = isStale,
                callbacks = callbacks,
            ),
        )

    private fun tabs(ready: EarnProductState.Ready): ImmutableList<EarnInfoTab> = EarnInfoTab.entries.filter { tab ->
        when (tab) {
            EarnInfoTab.MyFunds -> true
            EarnInfoTab.Benefits -> ready.benefits.isNotEmpty()
            EarnInfoTab.Details -> ready.details.isNotEmpty()
        }
    }.toImmutableList()

    private fun EarnTextBlock.toBenefitUM() = EarnBenefitUM(icon = icon, title = title, description = description)

    /** The one place an amount of the position turns into text, so the header and the rows cannot differ. */
    private fun cryptoText(amount: BigDecimal, currency: CryptoCurrency, isBalanceHidden: Boolean): TextReference =
        stringReference(amount.format { crypto(currency) }.orMaskWithStars(isBalanceHidden))

    data class Callbacks(
        val onActionClick: (EarnAction) -> Unit,
        val onBlocked: (EarnBlock) -> Unit,
        val onInfoClick: (EarnTextBlock) -> Unit,
        val onLinkClick: (String) -> Unit,
        val onHowItWorksClick: (EarnHowItWorks) -> Unit,
        val onClose: () -> Unit,
    )
}