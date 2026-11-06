package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.tokens.TokenRowStateConverter
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chart_ascending_16
import com.tangem.core.ui.res.generated.icons.ic_clock_16
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletContentClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokensListItemUM2
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.api.overview.EarnHeadline
import com.tangem.features.earn.api.overview.EarnOverviews
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class WalletTokenRowConverter(
    appCurrency: AppCurrency,
    private val accountId: AccountId,
    private val clickIntents: WalletContentClickIntents,
    private val yieldModuleApyMap: Map<String, BigDecimal>,
    private val earnOverviews: EarnOverviews,
    private val userWalletId: UserWalletId,
    private val jointPendingOperations: List<JointPendingOperation> = emptyList(),
) {

    private val tokenRowStateConverter = TokenRowStateConverter(appCurrency)

    fun convert(value: CryptoCurrencyStatus, shouldShowPromo: Boolean): TokensListItemUM2.TokenV2 {
        val headline = earnOverviews[value.currency.id]?.headline
        val pendingOperationBubble = toPendingOperationBubble(value)
        val promo = if (pendingOperationBubble == null && shouldShowPromo) toYieldPromo(value) else null

        return TokensListItemUM2.TokenV2(
            state = tokenRowStateConverter.convert(
                currencyStatus = value,
                onClick = { clickIntents.onTokenItemClick(accountId, value) },
                badge = headline?.toBadge(),
                messageBubble = pendingOperationBubble ?: promo?.bubble,
            ),
            onLongClick = if (value.value is CryptoCurrencyStatus.Loading) {
                null
            } else {
                { offset, state ->
                    clickIntents.onTokenRowLongClick(
                        accountId = accountId,
                        cryptoCurrencyStatus = value,
                        offset = offset,
                        tokenRowState = state,
                    )
                }
            },
            onPromoShown = promo?.onShown,
        )
    }

    private fun EarnHeadline.toBadge() = TangemTokenRow.Badge(
        text = badge,
        status = if (isActive) badgeAccent.toStatus() else TangemBadge.Status.Neutral,
    )

    private fun EarnBadgeAccent.toStatus() = when (this) {
        EarnBadgeAccent.Info -> TangemBadge.Status.Info
        EarnBadgeAccent.Success -> TangemBadge.Status.Success
    }

    private fun toPendingOperationBubble(currencyStatus: CryptoCurrencyStatus): TangemTokenRow.MessageBubble? {
        val operation = jointPendingOperations.firstOrNull { it.transfer.currency.id == currencyStatus.currency.id }
            ?: return null

        return TangemTokenRow.MessageBubble(
            text = resourceReference(R.string.joint_account_pending_transaction_message),
            variant = TangemMessageBubble.Variant.Warning,
            icon = Icons.ic_clock_16,
            onClick = {
                clickIntents.onJointOperationBannerClick(
                    userWalletId = userWalletId,
                    currency = operation.transfer.currency,
                    operationId = operation.operationId,
                )
            },
        )
    }

    private fun toYieldPromo(currencyStatus: CryptoCurrencyStatus): YieldPromo? {
        val currency = currencyStatus.currency
        val yieldApyPercent = yieldApyPercentOf(currency, yieldModuleApyMap)
        if (currencyStatus.value !is CryptoCurrencyStatus.Loaded || yieldApyPercent == null) return null

        return YieldPromo(
            bubble = TangemTokenRow.MessageBubble(
                text = resourceReference(
                    R.string.yield_module_main_screen_promo_banner_message,
                    wrappedList(yieldApyPercent.movePointLeft(YIELD_PERCENT_POINT_SHIFT).formatEarnRate()),
                ),
                variant = TangemMessageBubble.Variant.Success,
                icon = Icons.ic_chart_ascending_16,
                onClick = {
                    clickIntents.onYieldPromoClicked(accountId, currencyStatus, yieldApyPercent.toPlainString())
                },
                onClose = clickIntents::onYieldPromoCloseClick,
            ),
            onShown = { clickIntents.onYieldPromoShown(currency) },
        )
    }

    private class YieldPromo(val bubble: TangemTokenRow.MessageBubble, val onShown: () -> Unit)
}