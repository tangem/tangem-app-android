package com.tangem.tap.features.walletSelector.ui

import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.toFormattedFiatValue
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.walletSelector.redux.UserWalletModel
import com.tangem.tap.features.walletSelector.redux.WalletSelectorState
import com.tangem.tap.features.walletSelector.ui.model.MultiCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.SingleCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.UserWalletItem
import java.math.BigDecimal

internal fun WalletSelectorScreenState.updateWithNewState(
    newState: WalletSelectorState,
): WalletSelectorScreenState {
    val walletsUi = newState.wallets.toUiModels(newState.fiatCurrency)
    val walletsIds = walletsUi.map { it.id }
    val multiCurrencyWallets = arrayListOf<MultiCurrencyUserWalletItem>()
    val singleCurrencyWallets = arrayListOf<SingleCurrencyUserWalletItem>()
    walletsUi.forEach { wallet ->
        when (wallet) {
            is MultiCurrencyUserWalletItem -> {
                multiCurrencyWallets.add(wallet)
            }
            is SingleCurrencyUserWalletItem -> {
                singleCurrencyWallets.add(wallet)
            }
        }
    }

    return this.copy(
        multiCurrencyWallets = multiCurrencyWallets,
        singleCurrencyWallets = singleCurrencyWallets,
        selectedUserWalletId = newState.selectedWalletId,
        editingUserWalletsIds = editingUserWalletsIds.filter { it in walletsIds },
        isLocked = newState.isLocked,
        showUnlockProgress = newState.isUnlockInProgress,
        showAddCardProgress = newState.isCardSavingInProgress,
        error = newState.error
            ?.takeUnless { it.silent }
            ?.let { error ->
                error.messageResId?.let { TextReference.Res(it) }
                    ?: TextReference.Str(error.customMessage)
            },
    )
}

private fun List<UserWalletModel>.toUiModels(
    appCurrency: FiatCurrency,
): Sequence<UserWalletItem> {
    return this.asSequence().map { userWalletModel ->
        with(userWalletModel) {
            val formatAmount = { amount: BigDecimal ->
                amount.toFormattedFiatValue(appCurrency.symbol)
            }
            val balance = when (fiatBalance) {
                is TotalFiatBalance.Error -> UserWalletItem.Balance.Error(formatAmount(fiatBalance.amount))
                is TotalFiatBalance.Loaded -> UserWalletItem.Balance.Loaded(formatAmount(fiatBalance.amount))
                is TotalFiatBalance.Loading -> UserWalletItem.Balance.Loading
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
