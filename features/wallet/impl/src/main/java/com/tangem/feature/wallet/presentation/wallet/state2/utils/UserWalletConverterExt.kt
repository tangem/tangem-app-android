package com.tangem.feature.wallet.presentation.wallet.state2.utils

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState

internal inline fun UserWallet.createStateByWalletType(
    multiCurrencyCreator: () -> WalletState.MultiCurrency,
    singleCurrencyCreator: () -> WalletState.SingleCurrency,
): WalletState {
    return if (isWalletWithTokens()) multiCurrencyCreator() else singleCurrencyCreator()
}

private fun UserWallet.isWalletWithTokens(): Boolean {
    return isMultiCurrency || scanResponse.cardTypesResolver.isSingleWalletWithToken()
}
