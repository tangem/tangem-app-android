package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Provider
import com.tangem.core.ui.utils.BigDecimalFormatter.formatFiatAmount
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.model.TokenList.FiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.utils.converter.Converter

internal class FiatBalanceToWalletCardConverter(
    private val currentState: WalletCardState,
    private val cardTypeResolverProvider: Provider<CardTypesResolver>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val currentWalletProvider: Provider<UserWallet>,
    private val isWalletContentHidden: Boolean,
) : Converter<FiatBalance, WalletCardState> {

    override fun convert(value: FiatBalance): WalletCardState {
        return when (value) {
            is FiatBalance.Loading -> currentState.toLoadingWalletCardState()
            is FiatBalance.Failed -> currentState.toErrorWalletCardState()
            is FiatBalance.Loaded -> value.convertToWalletCardState()
        }
    }

    private fun WalletCardState.toLoadingWalletCardState(): WalletCardState {
        return WalletCardState.Loading(id, title, additionalInfo, imageResId, onRenameClick, onDeleteClick)
    }

    private fun WalletCardState.toErrorWalletCardState(): WalletCardState {
        return WalletCardState.Error(
            id = id,
            title = title,
            imageResId = imageResId,
            onDeleteClick = onDeleteClick,
            onRenameClick = onRenameClick,
            additionalInfo = WalletAdditionalInfoFactory.resolve(
                cardTypesResolver = cardTypeResolverProvider(),
                wallet = currentWalletProvider(),
            ),
        )
    }

    private fun FiatBalance.Loaded.convertToWalletCardState(): WalletCardState {
        return if (isWalletContentHidden) {
            WalletCardState.HiddenContent(
                id = currentState.id,
                title = currentState.title,
                additionalInfo = currentState.additionalInfo ?: WalletCardState.HIDDEN_BALANCE_TEXT,
                imageResId = currentState.imageResId,
                onRenameClick = currentState.onRenameClick,
                onDeleteClick = currentState.onDeleteClick,
            )
        } else {
            val appCurrency = appCurrencyProvider()

            WalletCardState.Content(
                id = currentState.id,
                title = currentState.title,
                additionalInfo = WalletAdditionalInfoFactory.resolve(
                    cardTypesResolver = cardTypeResolverProvider(),
                    wallet = currentWalletProvider(),
                ),
                imageResId = currentState.imageResId,
                onRenameClick = currentState.onRenameClick,
                onDeleteClick = currentState.onDeleteClick,
                balance = formatFiatAmount(
                    fiatAmount = this.amount,
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                ),
            )
        }
    }
}