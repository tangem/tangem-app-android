package com.tangem.common.ui.tokens

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.marketprice.utils.PriceChangeConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

/**
 * Token item state converter from [CryptoCurrencyStatus] to [TokenItemState]
 *
 * @property appCurrency           app currency
 * @property titleStateProvider    title state provider
 * @property subtitleStateProvider subtitle state provider
 * @property onItemClick           callback is invoked when item is clicked
 * @property onItemLongClick       callback is invoked when item is long clicked
 */
class TokenItemStateConverter(
    private val appCurrency: AppCurrency,
    private val iconStateProvider: (CryptoCurrencyStatus) -> CurrencyIconState = {
        CryptoCurrencyToIconStateConverter().convert(it)
    },
    private val titleStateProvider: (CryptoCurrencyStatus) -> TokenItemState.TitleState = Companion::createTitleState,
    private val subtitleStateProvider: (CryptoCurrencyStatus) -> TokenItemState.SubtitleState? = {
        createSubtitleState(it, appCurrency)
    },
    private val fiatAmountStateProvider: (CryptoCurrencyStatus) -> TokenItemState.FiatAmountState? = {
        createFiatAmountState(it, appCurrency)
    },
    private val onItemClick: ((TokenItemState, CryptoCurrencyStatus) -> Unit)? = null,
    private val onItemLongClick: ((TokenItemState, CryptoCurrencyStatus) -> Unit)? = null,
) : Converter<CryptoCurrencyStatus, TokenItemState> {

    override fun convert(value: CryptoCurrencyStatus): TokenItemState {
        return when (value.value) {
            is CryptoCurrencyStatus.Loading -> value.mapToLoadingState()
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> value.mapToTokenItemState()
            is CryptoCurrencyStatus.MissedDerivation -> value.mapToNoAddressTokenItemState()
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            -> value.mapToUnreachableTokenItemState()
        }
    }

    private fun CryptoCurrencyStatus.mapToLoadingState(): TokenItemState.Loading {
        return TokenItemState.Loading(
            id = currency.id.value,
            iconState = iconStateProvider(this),
            titleState = titleStateProvider(this) as TokenItemState.TitleState.Content,
            subtitleState = requireNotNull(subtitleStateProvider(this)),
        )
    }

    private fun CryptoCurrencyStatus.mapToTokenItemState(): TokenItemState.Content {
        return TokenItemState.Content(
            id = currency.id.value,
            iconState = iconStateProvider(this),
            titleState = titleStateProvider(this),
            subtitleState = requireNotNull(subtitleStateProvider(this)),
            fiatAmountState = requireNotNull(fiatAmountStateProvider(this)),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(text = getFormattedAmount()),
            onItemClick = onItemClick?.let { onItemClick ->
                { onItemClick(it, this) }
            },
            onItemLongClick = onItemLongClick?.let { onItemLongClick ->
                { onItemLongClick(it, this) }
            },
        )
    }

    private fun CryptoCurrencyStatus.getFormattedAmount(): String {
        val amount = value.amount?.plus(getStakedBalance()) ?: return DASH_SIGN

        return amount.format { crypto(currency) }
    }

    private fun CryptoCurrencyStatus.mapToUnreachableTokenItemState(): TokenItemState.Unreachable {
        return TokenItemState.Unreachable(
            id = currency.id.value,
            iconState = iconStateProvider(this),
            titleState = titleStateProvider(this),
            subtitleState = subtitleStateProvider(this),
            onItemClick = onItemClick?.let { onItemClick ->
                { onItemClick(it, this) }
            },
            onItemLongClick = onItemLongClick?.let { onItemLongClick ->
                { onItemLongClick(it, this) }
            },
        )
    }

    private fun CryptoCurrencyStatus.mapToNoAddressTokenItemState(): TokenItemState.NoAddress {
        return TokenItemState.NoAddress(
            id = currency.id.value,
            iconState = iconStateProvider(this),
            titleState = titleStateProvider(this),
            subtitleState = subtitleStateProvider(this),
            onItemLongClick = onItemLongClick?.let { onItemLongClick ->
                { onItemLongClick(it, this) }
            },
        )
    }

    companion object {

        private fun createTitleState(currencyStatus: CryptoCurrencyStatus): TokenItemState.TitleState {
            return when (val value = currencyStatus.value) {
                is CryptoCurrencyStatus.Loading,
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                -> {
                    TokenItemState.TitleState.Content(text = currencyStatus.currency.name)
                }
                is CryptoCurrencyStatus.Loaded,
                is CryptoCurrencyStatus.Custom,
                is CryptoCurrencyStatus.NoQuote,
                is CryptoCurrencyStatus.NoAccount,
                -> {
                    TokenItemState.TitleState.Content(
                        text = currencyStatus.currency.name,
                        hasPending = value.hasCurrentNetworkTransactions,
                    )
                }
            }
        }

        private fun createSubtitleState(
            currencyStatus: CryptoCurrencyStatus,
            appCurrency: AppCurrency,
        ): TokenItemState.SubtitleState? {
            return when (currencyStatus.value) {
                is CryptoCurrencyStatus.Loading -> TokenItemState.SubtitleState.Loading
                is CryptoCurrencyStatus.Loaded,
                is CryptoCurrencyStatus.Custom,
                is CryptoCurrencyStatus.NoQuote,
                is CryptoCurrencyStatus.NoAccount,
                -> currencyStatus.getCryptoPriceState(appCurrency)
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                -> null
            }
        }

        private fun createFiatAmountState(
            status: CryptoCurrencyStatus,
            appCurrency: AppCurrency,
        ): TokenItemState.FiatAmountState? {
            return when (status.value) {
                is CryptoCurrencyStatus.Loaded,
                is CryptoCurrencyStatus.Custom,
                is CryptoCurrencyStatus.NoQuote,
                is CryptoCurrencyStatus.NoAccount,
                -> {
                    TokenItemState.FiatAmountState.Content(
                        text = status.getFormattedFiatAmount(appCurrency),
                        hasStaked = !status.getStakedBalance().isZero(),
                    )
                }
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.Loading,
                -> null
            }
        }

        private fun CryptoCurrencyStatus.getCryptoPriceState(appCurrency: AppCurrency): TokenItemState.SubtitleState {
            val fiatRate = value.fiatRate
            val priceChange = value.priceChange

            return if (fiatRate != null && priceChange != null) {
                TokenItemState.SubtitleState.CryptoPriceContent(
                    price = fiatRate.getFormattedCryptoPrice(appCurrency),
                    priceChangePercent = priceChange.format { percent() },
                    type = priceChange.getPriceChangeType(),
                )
            } else {
                TokenItemState.SubtitleState.Unknown
            }
        }

        private fun BigDecimal.getFormattedCryptoPrice(appCurrency: AppCurrency): String {
            return BigDecimalFormatter.formatFiatAmountUncapped(
                fiatAmount = this,
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
        }

        private fun BigDecimal.getPriceChangeType(): PriceChangeType {
            return PriceChangeConverter.fromBigDecimal(value = this)
        }

        fun CryptoCurrencyStatus.getFormattedFiatAmount(appCurrency: AppCurrency): String {
            val fiatYieldBalance = value.fiatRate?.times(getStakedBalance()).orZero()
            val fiatAmount = value.fiatAmount?.plus(fiatYieldBalance) ?: return DASH_SIGN

            return BigDecimalFormatter.formatFiatAmount(fiatAmount, appCurrency.code, appCurrency.symbol)
        }

        private fun CryptoCurrencyStatus.getStakedBalance() =
            (value.yieldBalance as? YieldBalance.Data)?.getTotalWithRewardsStakingBalance().orZero()
    }
}