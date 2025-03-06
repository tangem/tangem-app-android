package com.tangem.common.ui.tokens

import com.tangem.common.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.marketprice.utils.PriceChangeConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.toImmutableList
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
    private val subtitle2StateProvider: (CryptoCurrencyStatus) -> TokenItemState.Subtitle2State? = {
        createSubtitle2State(status = it)
    },
    private val fiatAmountStateProvider: (CryptoCurrencyStatus) -> TokenItemState.FiatAmountState? = {
        createFiatAmountState(status = it, appCurrency = appCurrency)
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
            subtitle2State = requireNotNull(subtitle2StateProvider(this)),
            onItemClick = onItemClick?.let { onItemClick ->
                { onItemClick(it, this) }
            },
            onItemLongClick = onItemLongClick?.let { onItemLongClick ->
                { onItemLongClick(it, this) }
            },
        )
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

        fun CryptoCurrencyStatus.getFormattedFiatAmount(appCurrency: AppCurrency, includeStaking: Boolean): String {
            val fiatAmount = value.fiatAmount ?: return DASH_SIGN

            val totalAmount = if (includeStaking) {
                val fiatYieldBalance = value.fiatRate?.times(getStakedBalance()).orZero()

                fiatAmount.plus(fiatYieldBalance)
            } else {
                fiatAmount
            }

            return totalAmount.format {
                fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
            }
        }

        fun CryptoCurrencyStatus.getFormattedCryptoAmount(includeStaking: Boolean): String {
            val cryptoAmount = value.amount ?: return DASH_SIGN

            val totalAmount = if (includeStaking) {
                cryptoAmount.plus(getStakedBalance())
            } else {
                cryptoAmount
            }

            return totalAmount.format { crypto(currency) }
        }

        private fun CryptoCurrencyStatus.getStakedBalance() =
            (value.yieldBalance as? YieldBalance.Data)?.getTotalWithRewardsStakingBalance().orZero()

        private fun createTitleState(currencyStatus: CryptoCurrencyStatus): TokenItemState.TitleState {
            return when (val value = currencyStatus.value) {
                is CryptoCurrencyStatus.Loading,
                is CryptoCurrencyStatus.MissedDerivation,
                is CryptoCurrencyStatus.Unreachable,
                is CryptoCurrencyStatus.NoAmount,
                -> {
                    TokenItemState.TitleState.Content(text = stringReference(currencyStatus.currency.name))
                }
                is CryptoCurrencyStatus.Loaded,
                is CryptoCurrencyStatus.Custom,
                is CryptoCurrencyStatus.NoQuote,
                is CryptoCurrencyStatus.NoAccount,
                -> {
                    TokenItemState.TitleState.Content(
                        text = stringReference(currencyStatus.currency.name),
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

        private fun createSubtitle2State(status: CryptoCurrencyStatus): TokenItemState.Subtitle2State? {
            return when (status.value) {
                is CryptoCurrencyStatus.Loaded,
                is CryptoCurrencyStatus.Custom,
                is CryptoCurrencyStatus.NoQuote,
                is CryptoCurrencyStatus.NoAccount,
                -> {
                    TokenItemState.Subtitle2State.TextContent(
                        text = status.getFormattedCryptoAmount(includeStaking = true),
                        isFlickering = status.value.isFlickering(),
                    )
                }
                is CryptoCurrencyStatus.Loading,
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
                        text = status.getFormattedFiatAmount(appCurrency = appCurrency, includeStaking = true),
                        isFlickering = status.value.isFlickering(),
                        icons = buildList {
                            if (!status.getStakedBalance().isZero()) {
                                TokenItemState.FiatAmountState.Content.IconUM(
                                    iconRes = R.drawable.ic_staking_24,
                                    useAccentColor = true,
                                ).let(::add)
                            }
                            if (status.value.sources.total == StatusSource.ONLY_CACHE) {
                                TokenItemState.FiatAmountState.Content.IconUM(
                                    iconRes = R.drawable.ic_error_sync_24,
                                    useAccentColor = false,
                                ).let(::add)
                            }
                        }.toImmutableList(),
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
                    isFlickering = value.isFlickering(),
                )
            } else {
                TokenItemState.SubtitleState.Unknown
            }
        }

        private fun BigDecimal.getFormattedCryptoPrice(appCurrency: AppCurrency): String {
            return format {
                fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
            }
        }

        private fun BigDecimal.getPriceChangeType(): PriceChangeType {
            return PriceChangeConverter.fromBigDecimal(value = this)
        }

        fun CryptoCurrencyStatus.Value.isFlickering(): Boolean = sources.total == StatusSource.CACHE
    }
}