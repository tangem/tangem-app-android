package com.tangem.feature.swap.converters

import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.common.ui.tokens.TokenItemStateConverter.Companion.isFlickering
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.ui.AccountSwapAvailability
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class AccountTokenItemConverter(
    private val appCurrency: AppCurrency,
    private val unavailableErrorText: TextReference,
    private val onItemClick: (String) -> Unit,
) : Converter<AccountSwapAvailability, TokensListItemUM.Portfolio> {

    override fun convert(value: AccountSwapAvailability): TokensListItemUM.Portfolio {
        return TokensListItemUM.Portfolio(
            tokenItemUM = AccountCryptoPortfolioItemStateConverter(
                appCurrency = appCurrency,
                account = value.account,
                onItemClick = null,
            ).convert(TotalFiatBalance.Failed),
            isExpanded = true,
            isCollapsable = false,
            tokens = value.currencyList.map { accountSwapCurrency ->
                if (accountSwapCurrency.isAvailable) {
                    createAvailableItemConverter()
                } else {
                    createUnavailableItemConverter()
                }.convert(accountSwapCurrency.cryptoCurrencyStatus)
            }.map(TokensListItemUM::Token).toPersistentList(),
        )
    }

    fun createAvailableItemConverter(): TokenItemStateConverter {
        return TokenItemStateConverter(
            appCurrency = appCurrency,
            subtitleStateProvider = { status ->
                createSubtitleState(
                    status = status,
                    isAvailable = true,
                    text = stringReference(value = status.currency.symbol),
                )
            },
            subtitle2StateProvider = ::createSubtitle2State,
            fiatAmountStateProvider = {
                createFiatAmountStateProvider(status = it, appCurrency = appCurrency, isAvailable = true)
            },
            onItemClick = { account, currencyStatus -> onItemClick(currencyStatus.currency.id.value) },
        )
    }

    fun createUnavailableItemConverter(): TokenItemStateConverter {
        return TokenItemStateConverter(
            appCurrency = appCurrency,
            iconStateProvider = { CryptoCurrencyToIconStateConverter(isAvailable = false).convert(it) },
            titleStateProvider = { status ->
                TokenItemState.TitleState.Content(
                    text = stringReference(value = status.currency.name),
                    isAvailable = false,
                )
            },
            subtitleStateProvider = { status ->
                createSubtitleState(
                    status = status,
                    isAvailable = false,
                    text = unavailableErrorText,
                )
            },
            subtitle2StateProvider = ::createSubtitle2State,
            fiatAmountStateProvider = {
                createFiatAmountStateProvider(status = it, appCurrency = appCurrency, isAvailable = false)
            },
        )
    }

    private fun createSubtitleState(
        status: CryptoCurrencyStatus,
        isAvailable: Boolean,
        text: TextReference,
    ): TokenItemState.SubtitleState {
        return when (status.value) {
            CryptoCurrencyStatus.Loading -> TokenItemState.SubtitleState.Loading
            else -> {
                TokenItemState.SubtitleState.TextContent(
                    value = text,
                    isAvailable = isAvailable,
                )
            }
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
                    text = status.getTotalCryptoAmount().format {
                        crypto(cryptoCurrency = status.currency)
                    },
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

    private fun createFiatAmountStateProvider(
        status: CryptoCurrencyStatus,
        appCurrency: AppCurrency,
        isAvailable: Boolean,
    ): TokenItemState.FiatAmountState? {
        return when (status.value) {
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> {
                TokenItemState.FiatAmountState.TextContent(
                    text = status.getTotalFiatAmount().format {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                        )
                    },
                    isAvailable = isAvailable,
                    isFlickering = status.value.isFlickering(),
                )
            }
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Loading,
            -> null
        }
    }
}