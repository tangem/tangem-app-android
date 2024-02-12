package com.tangem.feature.wallet.presentation.wallet.state.utils

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState

internal inline fun UserWallet.createStateByWalletType(
    multiCurrencyCreator: () -> WalletState.MultiCurrency,
    singleCurrencyCreator: () -> WalletState.SingleCurrency,
    visaWalletCreator: () -> WalletState.Visa,
): WalletState = when {
    isVisaWallet() -> visaWalletCreator()
    isWalletWithTokens() -> multiCurrencyCreator()
    else -> singleCurrencyCreator()
}

private fun UserWallet.isWalletWithTokens(): Boolean {
    return isMultiCurrency || scanResponse.cardTypesResolver.isSingleWalletWithToken()
}

private fun UserWallet.isVisaWallet(): Boolean {
    return scanResponse.cardTypesResolver.isVisaWallet()
}
