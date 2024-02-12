package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.getCardsCount
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.utils.converter.Converter

internal class MultiWalletCardStateConverter(
    private val fiatBalance: TokenList.FiatBalance,
    private val selectedWallet: UserWallet,
    private val appCurrency: AppCurrency,
) : Converter<WalletCardState, WalletCardState> {

    override fun convert(value: WalletCardState): WalletCardState {
        return when (fiatBalance) {
            is TokenList.FiatBalance.Loading -> value.toLoadingState()
            is TokenList.FiatBalance.Failed -> value.toErrorState()
            is TokenList.FiatBalance.Loaded -> value.toWalletCardState(fiatBalance)
        }
    }

    private fun WalletCardState.toLoadingState(): WalletCardState {
        return WalletCardState.Loading(
            id = id,
            title = title,
            additionalInfo = additionalInfo,
            imageResId = imageResId,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
        )
    }

    private fun WalletCardState.toErrorState(): WalletCardState {
        return WalletCardState.Error(
            id = id,
            title = title,
            additionalInfo = additionalInfo,
            imageResId = imageResId,
            onDeleteClick = onDeleteClick,
            onRenameClick = onRenameClick,
        )
    }

    private fun WalletCardState.toWalletCardState(fiatBalance: TokenList.FiatBalance.Loaded): WalletCardState {
        return WalletCardState.Content(
            id = id,
            title = title,
            additionalInfo = WalletAdditionalInfoFactory.resolve(wallet = selectedWallet),
            imageResId = imageResId,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
            balance = BigDecimalFormatter.formatFiatAmount(
                fiatAmount = fiatBalance.amount,
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            ),
            cardCount = selectedWallet.getCardsCount(),
        )
    }
}
