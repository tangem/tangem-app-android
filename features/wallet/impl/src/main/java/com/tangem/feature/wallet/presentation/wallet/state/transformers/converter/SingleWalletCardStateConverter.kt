package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.common.util.getCardsCount
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero

internal class SingleWalletCardStateConverter(
    private val status: CryptoCurrencyStatus.Value,
    private val selectedWallet: UserWallet,
    private val appCurrency: AppCurrency,
) : Converter<WalletCardState, WalletCardState> {

    override fun convert(value: WalletCardState): WalletCardState {
        return when (status) {
            is CryptoCurrencyStatus.Loading -> value.toLoadingState()
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            -> value.toErrorState()
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.NoAmount,
            -> value.toContentState(status)
        }
    }

    private fun WalletCardState.toLoadingState(): WalletCardState {
        return WalletCardState.Loading(
            id = id,
            title = title,
            imageResId = imageResId,
            dropDownItems = dropDownItems,
        )
    }

    private fun WalletCardState.toErrorState(): WalletCardState {
        return WalletCardState.Error(
            id = id,
            title = title,
            imageResId = imageResId,
            dropDownItems = dropDownItems,
        )
    }

    private fun WalletCardState.toContentState(status: CryptoCurrencyStatus.Value): WalletCardState {
        return WalletCardState.Content(
            id = id,
            title = title,
            additionalInfo = WalletAdditionalInfoFactory.resolve(
                wallet = selectedWallet,
                currencyAmount = status.amount,
            ),
            imageResId = imageResId,
            dropDownItems = dropDownItems,
            balance = formatFiatAmount(status = status, appCurrency = appCurrency),
            cardCount = when (selectedWallet) {
                is UserWallet.Cold -> selectedWallet.getCardsCount()
                is UserWallet.Hot -> null
            },
            isZeroBalance = status.fiatAmount?.isZero(),
            isBalanceFlickering = (status as? CryptoCurrencyStatus.Loaded)?.sources?.total == StatusSource.CACHE,
        )
    }

    private fun formatFiatAmount(status: CryptoCurrencyStatus.Value, appCurrency: AppCurrency): String {
        val fiatAmount = status.fiatAmount ?: return DASH_SIGN

        return fiatAmount.format {
            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
        }
    }
}