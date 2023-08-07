package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Provider
import com.tangem.core.ui.utils.BigDecimalFormatter.formatFiatAmount
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.utils.converter.Converter

internal class FiatBalanceToWalletCardConverter(
    private val currentState: WalletCardState,
    private val cardTypeResolverProvider: Provider<CardTypesResolver>,
    private val isLockedState: Boolean,
    private val isWalletContentHidden: Boolean,
    private val fiatCurrencyCode: String,
    private val fiatCurrencySymbol: String,
) : Converter<TokenList.FiatBalance, WalletCardState> {

    override fun convert(value: TokenList.FiatBalance): WalletCardState {
        val additionalInfo = WalletAdditionalInfoFactory.resolve(
            cardTypesResolver = cardTypeResolverProvider(),
            isLocked = isLockedState,
        )
        return when (value) {
            is TokenList.FiatBalance.Loading -> with(currentState) {
                WalletCardState.Loading(id, title, additionalInfo, imageResId, onClick)
            }
            is TokenList.FiatBalance.Failed -> with(currentState) {
                WalletCardState.Error(id, title, additionalInfo, imageResId, onClick)
            }
            is TokenList.FiatBalance.Loaded -> with(currentState) {
                if (isWalletContentHidden) {
                    WalletCardState.HiddenContent(id, title, additionalInfo, imageResId, onClick)
                } else {
                    WalletCardState.Content(
                        id = id,
                        title = title,
                        additionalInfo = additionalInfo,
                        imageResId = imageResId,
                        onClick = onClick,
                        balance = formatFiatAmount(value.amount, fiatCurrencyCode, fiatCurrencySymbol),
                    )
                }
            }
        }
    }
}
