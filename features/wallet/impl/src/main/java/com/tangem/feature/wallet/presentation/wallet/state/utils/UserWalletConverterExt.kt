package com.tangem.feature.wallet.presentation.wallet.state.utils

import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState

internal inline fun UserWallet.createStateByWalletType(
    multiCurrencyCreator: () -> WalletState.MultiCurrency,
    singleCurrencyCreator: () -> WalletState.SingleCurrency,
): WalletState = when (this) {
    is UserWallet.Cold -> when {
        isWalletWithTokens() -> multiCurrencyCreator()
        else -> singleCurrencyCreator()
    }
    is UserWallet.Hot -> multiCurrencyCreator()
}

private fun UserWallet.Cold.isWalletWithTokens(): Boolean {
    return isMultiCurrency || scanResponse.cardTypesResolver.isSingleWalletWithToken()
}

internal fun UserWallet.isSingleWallet(): Boolean {
    return this is UserWallet.Cold && scanResponse.cardTypesResolver.isSingleWallet()
}

internal fun UserWallet.isSingleWalletWithToken(): Boolean {
    return this is UserWallet.Cold && scanResponse.cardTypesResolver.isSingleWalletWithToken()
}