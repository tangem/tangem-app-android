package com.tangem.feature.wallet.presentation.wallet.state.utils

import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState

internal inline fun UserWallet.createStateByWalletType(
    multiCurrencyCreator: () -> WalletState.MultiCurrency,
    singleCurrencyCreator: () -> WalletState.SingleCurrency,
    visaWalletCreator: () -> WalletState.Visa,
): WalletState = when (this) {
    is UserWallet.Cold -> when {
        isVisaWallet() -> visaWalletCreator()
        isWalletWithTokens() -> multiCurrencyCreator()
        else -> singleCurrencyCreator()
    }
    is UserWallet.Hot -> multiCurrencyCreator()
}

private fun UserWallet.Cold.isWalletWithTokens(): Boolean {
    return isMultiCurrency || scanResponse.cardTypesResolver.isSingleWalletWithToken()
}

private fun UserWallet.Cold.isVisaWallet(): Boolean {
    return scanResponse.cardTypesResolver.isVisaWallet()
}