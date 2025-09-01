package com.tangem.features.onramp.tokenlist.entity.utils

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.common.ui.tokens.TokenItemStateConverter.Companion.getFormattedCryptoAmount
import com.tangem.common.ui.tokens.TokenItemStateConverter.Companion.getFormattedFiatAmount
import com.tangem.common.ui.tokens.TokenItemStateConverter.Companion.isFlickering
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus

/**
[REDACTED_AUTHOR]
 */
internal object OnrampTokenItemStateConverterFactory {

    fun createAvailableItemConverter(
        appCurrency: AppCurrency,
        onItemClick: (TokenItemState, CryptoCurrencyStatus) -> Unit,
    ): TokenItemStateConverter {
        return TokenItemStateConverter(
            appCurrency = appCurrency,
            subtitleStateProvider = { createSubtitleState(status = it, isAvailable = true) },
            subtitle2StateProvider = ::createSubtitle2State,
            fiatAmountStateProvider = {
                createFiatAmountStateProvider(status = it, appCurrency = appCurrency, isAvailable = true)
            },
            onItemClick = onItemClick,
        )
    }

    fun createUnavailableItemConverter(appCurrency: AppCurrency): TokenItemStateConverter {
        return TokenItemStateConverter(
            appCurrency = appCurrency,
            iconStateProvider = { CryptoCurrencyToIconStateConverter(isAvailable = false).convert(it) },
            titleStateProvider = {
                TokenItemState.TitleState.Content(
                    text = stringReference(value = it.currency.name),
                    isAvailable = false,
                )
            },
            subtitleStateProvider = { createSubtitleState(status = it, isAvailable = false) },
            subtitle2StateProvider = ::createSubtitle2State,
            fiatAmountStateProvider = {
                createFiatAmountStateProvider(status = it, appCurrency = appCurrency, isAvailable = false)
            },
        )
    }

    private fun createSubtitleState(status: CryptoCurrencyStatus, isAvailable: Boolean): TokenItemState.SubtitleState {
        return when (status.value) {
            CryptoCurrencyStatus.Loading -> TokenItemState.SubtitleState.Loading
            else -> {
                TokenItemState.SubtitleState.TextContent(
                    value = stringReference(value = status.currency.symbol),
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
                    text = status.getFormattedCryptoAmount(),
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
                    text = status.getFormattedFiatAmount(appCurrency = appCurrency),
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