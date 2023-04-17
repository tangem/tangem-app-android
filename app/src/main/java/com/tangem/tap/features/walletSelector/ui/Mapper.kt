package com.tangem.tap.features.walletSelector.ui

import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.toFormattedFiatValue
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.features.walletSelector.redux.UserWalletModel
import com.tangem.tap.features.walletSelector.ui.model.MultiCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.SingleCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.UserWalletItem

internal fun List<UserWalletModel>.toUiModels(appCurrency: FiatCurrency): Sequence<UserWalletItem> {
    return this.asSequence().map { userWalletModel ->
        with(userWalletModel) {
            val balance = when (fiatBalance) {
                is TotalFiatBalance.Failed -> UserWalletItem.Balance.Failed
                is TotalFiatBalance.Loading -> UserWalletItem.Balance.Loading
                is TotalFiatBalance.Loaded -> UserWalletItem.Balance.Loaded(
                    amount = fiatBalance.amount.toFormattedFiatValue(appCurrency.symbol, appCurrency.code),
                    showWarning = fiatBalance.isWarning,
                )
            }
            when (type) {
                is UserWalletModel.Type.MultiCurrency -> MultiCurrencyUserWalletItem(
                    id = id,
                    name = name,
                    imageUrl = artworkUrl,
                    balance = balance,
                    isLocked = isLocked,
                    cardsInWallet = type.cardsInWallet,
                    tokensCount = type.tokensCount,
                )
                is UserWalletModel.Type.SingleCurrency -> SingleCurrencyUserWalletItem(
                    id = id,
                    name = name,
                    imageUrl = artworkUrl,
                    balance = balance,
                    isLocked = isLocked,
                    tokenName = type.blockchainName,
                )
            }
        }
    }
}
