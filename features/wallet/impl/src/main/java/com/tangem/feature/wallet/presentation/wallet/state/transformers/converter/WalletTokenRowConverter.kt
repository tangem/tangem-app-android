package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chart_ascending_16
import com.tangem.core.ui.res.generated.icons.ic_clock_16
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.feature.wallet.child.wallet.model.intents.WalletContentClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokensListItemUM2
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class WalletTokenRowConverter(
    private val appCurrency: AppCurrency,
    private val accountId: AccountId,
    private val shouldShowPromo: Boolean,
    private val clickIntents: WalletContentClickIntents,
    yieldModuleApyMap: Map<String, BigDecimal>,
    stakingAvailabilityMap: Map<CryptoCurrency, StakingAvailability>,
    private val userWalletId: UserWalletId,
    private val jointPendingOperations: List<JointPendingOperation> = emptyList(),
) : Converter<CryptoCurrencyStatus, TokensListItemUM2.TokenV2> {

    private val currencyToIconStateConverter = CryptoCurrencyToIconStateConverter()
    private val earnApyConverter = EarnApyConverter(
        yieldModuleApyMap = yieldModuleApyMap,
        stakingApyMap = stakingAvailabilityMap,
    )

    override fun convert(value: CryptoCurrencyStatus): TokensListItemUM2.TokenV2 {
        val earnApyInfo = earnApyConverter.convert(value)
        val promo = toYieldPromo(value, earnApyInfo.takeIf { shouldShowPromo })

        return TokensListItemUM2.TokenV2(
            state = toState(value, earnApyInfo, promo?.bubble),
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

    private fun toState(
        currencyStatus: CryptoCurrencyStatus,
        earnApyInfo: EarnApyConverter.EarnApyInfo?,
        promoBubble: TangemTokenRow.MessageBubble?,
    ): TangemTokenRow.State {
        val id = currencyStatus.currency.id.value
        val icon = currencyToIconStateConverter.convert(currencyStatus).toTokenIconState()
        val title = stringReference(currencyStatus.currency.name)
        val onClick = { clickIntents.onTokenItemClick(accountId, currencyStatus) }

        return when (val value = currencyStatus.value) {
            CryptoCurrencyStatus.Loading -> TangemTokenRow.State.Shimmer(id = id)
            is CryptoCurrencyStatus.Unreachable -> TangemTokenRow.State.Unreachable(
                id = id,
                icon = icon,
                title = title,
                badge = resourceReference(R.string.common_unreachable),
                onClick = onClick,
            )
            is CryptoCurrencyStatus.MissedDerivation -> TangemTokenRow.State.NoAddress(
                id = id,
                icon = icon,
                title = title,
                message = resourceReference(R.string.common_no_address),
            )
            is CryptoCurrencyStatus.NoAmount -> TangemTokenRow.State.Content(
                id = id,
                icon = icon,
                title = title,
                fiatBalance = stringReference(StringsSigns.DASH_SIGN),
                onClick = onClick,
            )
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> {
                val isFlickering = value.sources.total == StatusSource.CACHE
                val yieldSupply = value.yieldSupplyStatus
                TangemTokenRow.State.Content(
                    id = id,
                    icon = icon,
                    title = title,
                    badge = earnApyInfo?.toBadge(),
                    hasPending = value.hasCurrentNetworkTransactions,
                    quote = stringReference(
                        value.fiatRate.format {
                            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                        },
                    ),
                    priceChange = value.priceChange.orZero().toPriceChangeState(),
                    fiatBalance = stringReference(
                        currencyStatus.getTotalFiatAmount().format {
                            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                        },
                    ),
                    cryptoBalance = stringReference(
                        currencyStatus.getTotalCryptoAmount().format { crypto(currencyStatus.currency) },
                    ),
                    shouldShowContractWarning = yieldSupply?.isActive == true && !yieldSupply.isAllowedToSpend,
                    shouldShowUpdateWarning = value.sources.total == StatusSource.ONLY_CACHE,
                    isQuoteFlickering = isFlickering,
                    isBalanceFlickering = isFlickering,
                    messageBubble = toPendingOperationBubble(currencyStatus) ?: promoBubble,
                    onClick = onClick,
                )
            }
        }
    }

    private fun EarnApyConverter.EarnApyInfo.toBadge(): TangemTokenRow.Badge? {
        val text = text ?: return null
        return TangemTokenRow.Badge(
            text = text,
            status = when {
                !isActive -> TangemBadge.Status.Neutral
                isStaking -> TangemBadge.Status.Info
                else -> TangemBadge.Status.Success
            },
        )
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

    private fun toYieldPromo(
        currencyStatus: CryptoCurrencyStatus,
        earnApyInfo: EarnApyConverter.EarnApyInfo?,
    ): YieldPromo? {
        val currency = currencyStatus.currency
        val isLoadedToken = currency is CryptoCurrency.Token && currencyStatus.value is CryptoCurrencyStatus.Loaded
        if (!isLoadedToken || earnApyInfo == null) return null

        return YieldPromo(
            bubble = TangemTokenRow.MessageBubble(
                text = resourceReference(
                    R.string.yield_module_main_screen_promo_banner_message,
                    wrappedList(earnApyInfo.rate.formatEarnRate()),
                ),
                variant = TangemMessageBubble.Variant.Success,
                icon = Icons.ic_chart_ascending_16,
                onClick = {
                    val apyPercent = earnApyInfo.rate
                        .movePointRight(EarnApyConverter.PERCENT_POINT_SHIFT)
                        .toPlainString()
                    clickIntents.onYieldPromoClicked(accountId, currencyStatus, apyPercent)
                },
                onClose = clickIntents::onYieldPromoCloseClick,
            ),
            onShown = { clickIntents.onYieldPromoShown(currency) },
        )
    }

    private class YieldPromo(val bubble: TangemTokenRow.MessageBubble, val onShown: () -> Unit)
}