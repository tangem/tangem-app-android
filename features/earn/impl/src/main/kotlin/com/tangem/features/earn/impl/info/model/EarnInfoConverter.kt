package com.tangem.features.earn.impl.info.model

import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.common.ui.amountScreen.utils.getFiatString
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.earn.impl.guardedBy
import com.tangem.features.earn.impl.info.EarnInfoActions
import com.tangem.features.earn.impl.info.EarnInfoTexts
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab
import com.tangem.features.earn.impl.info.ui.state.EarnInfoUM
import com.tangem.features.earn.impl.picker.ui.state.EarnBenefitUM
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnFreshness
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnRewards
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.toUM
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.utils.StringsSigns.DASH_SIGN
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

internal class EarnInfoConverter @Inject constructor(
    private val actions: EarnInfoActions,
) {

    @Suppress("LongParameterList")
    fun convert(
        product: EarnProduct,
        status: CryptoCurrencyStatus,
        appCurrency: AppCurrency,
        ready: EarnProductState.Ready,
        callbacks: Callbacks,
        locale: Locale = Locale.getDefault(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): EarnInfoUM? {
        val position = (ready.position as? EarnPositionState.Known)?.position ?: return null
        val rate = status.value.fiatRate
        val staleSince = listOfNotNull(
            ready.freshness,
            position.freshness,
        ).filter { it.isStale }.minByOrNull { it.asOf }
        val isStale = staleSince != null

        return EarnInfoUM(
            title = product.title,
            subtitle = stringReference(status.currency.network.name),
            balanceFiat = getFiatReference(position.cryptoAmount, rate, appCurrency) ?: stringReference(DASH_SIGN),
            balanceCrypto = stringReference(position.cryptoAmount.format { crypto(status.currency) }),
            earned = position.earned?.let { earned ->
                EarnInfoTexts.earnedChip(getFiatString(earned, rate, appCurrency))
            },
            rewards = position.rewards?.let { rewards ->
                rewardsBanner(
                    rewards = rewards,
                    rate = rate,
                    appCurrency = appCurrency,
                    isStale = isStale,
                    callbacks = callbacks,
                )
            },
            // The numbers are as old as the oldest stale source
            staleNotice = staleSince?.let { EarnInfoTexts.staleNotice(it.formatAsOf(locale, zoneId)) },
            tabs = tabs(ready),
            myFunds = EarnInfoUM.MyFundsUM(
                provider = product.provider.title,
                providerIcon = product.provider.icon,
                description = position.description,
                rows = position.rows.map { it.toUM(callbacks.onInfoClick, callbacks.onLinkClick) }.toImmutableList(),
                // The informer modal cannot be built without entries
                onHowItWorksClick = ready.howItWorks
                    ?.takeIf { it.entries.isNotEmpty() }
                    ?.let { { callbacks.onHowItWorksClick(it) } },
            ),
            benefits = ready.benefits.map { it.toBenefitUM() }.toImmutableList(),
            details = ready.details.map { it.toUM(callbacks.onInfoClick, callbacks.onLinkClick) }.toImmutableList(),
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

    private fun rewardsBanner(
        rewards: EarnRewards,
        rate: BigDecimal?,
        appCurrency: AppCurrency,
        isStale: Boolean,
        callbacks: Callbacks,
    ) = EarnInfoUM.RewardsUM(
        title = EarnInfoTexts.rewardsTitle(getFiatString(rewards.amount, rate, appCurrency)),
        description = EarnInfoTexts.rewardsDescription,
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

    private fun EarnFreshness.formatAsOf(locale: Locale, zoneId: ZoneId): String =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(zoneId)
            .format(Instant.ofEpochMilli(asOf.toEpochMilliseconds()))

    data class Callbacks(
        val onActionClick: (EarnAction) -> Unit,
        val onBlocked: (EarnBlock) -> Unit,
        val onInfoClick: (EarnTextBlock) -> Unit,
        val onLinkClick: (String) -> Unit,
        val onHowItWorksClick: (EarnHowItWorks) -> Unit,
        val onClose: () -> Unit,
    )
}