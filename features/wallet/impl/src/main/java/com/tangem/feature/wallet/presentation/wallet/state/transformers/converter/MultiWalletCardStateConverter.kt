package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.util.getCardsCount
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.isZero

internal class MultiWalletCardStateConverter(
    private val fiatBalance: TotalFiatBalance,
    private val selectedWallet: UserWallet,
    private val appCurrency: AppCurrency,
) : Converter<WalletCardState, WalletCardState> {

    override fun convert(value: WalletCardState): WalletCardState {
        return when (fiatBalance) {
            is TotalFiatBalance.Loading -> value.toLoadingState()
            is TotalFiatBalance.Failed -> value.toErrorState()
            is TotalFiatBalance.Loaded -> value.toWalletCardState(fiatBalance)
        }
    }

    private fun WalletCardState.toLoadingState(): WalletCardState {
        return WalletCardState.Loading(
            id = id,
            title = title,
            additionalInfo = additionalInfo,
            imageResId = imageResId,
            dropDownItems = dropDownItems,
        )
    }

    private fun WalletCardState.toErrorState(): WalletCardState {
        return WalletCardState.Error(
            id = id,
            title = title,
            additionalInfo = additionalInfo,
            imageResId = imageResId,
            dropDownItems = dropDownItems,
        )
    }

    private fun WalletCardState.toWalletCardState(fiatBalance: TotalFiatBalance.Loaded): WalletCardState {
        return WalletCardState.Content(
            id = id,
            title = title,
            additionalInfo = WalletAdditionalInfoFactory.resolve(wallet = selectedWallet),
            imageResId = imageResId,
            dropDownItems = dropDownItems,
            balance = fiatBalance.amount.format {
                fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
            },
            isZeroBalance = fiatBalance.amount.isZero(),
            cardCount = when (selectedWallet) {
                is UserWallet.Cold -> selectedWallet.getCardsCount()
                is UserWallet.Hot -> null
            },
            isBalanceFlickering = fiatBalance.source == StatusSource.CACHE,
        )
    }
}