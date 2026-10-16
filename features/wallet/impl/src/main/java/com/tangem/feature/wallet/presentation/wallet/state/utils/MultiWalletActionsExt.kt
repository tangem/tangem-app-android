package com.tangem.feature.wallet.presentation.wallet.state.utils

import com.tangem.feature.wallet.presentation.wallet.state.model.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal fun WalletState.MultiCurrency.Content.enableButtons(): PersistentList<WalletManageButton> {
    return changeAvailability(enabled = true)
}

internal fun WalletState.MultiCurrency.Content.disableButtons(): PersistentList<WalletManageButton> {
    return changeAvailability(enabled = false)
}

private fun WalletState.MultiCurrency.Content.changeAvailability(enabled: Boolean): PersistentList<WalletManageButton> {
    return buttons
        .map { action ->
            when (action) {
                is WalletManageButton.Buy -> action.copy(isEnabled = enabled)
                is WalletManageButton.AddFunds -> action.copy(isEnabled = enabled)
                is WalletManageButton.Sell -> action.copy(isEnabled = enabled)
                is WalletManageButton.Transfer -> action.copy(isEnabled = enabled)
                is WalletManageButton.Swap -> action.copy(isEnabled = enabled)
                else -> action
            }
        }
        .toPersistentList()
}

internal fun WalletState.MultiCurrency.Content.showSwapBadge(showBadge: Boolean): PersistentList<WalletManageButton> {
    return buttons
        .map { action ->
            when (action) {
                is WalletManageButton.Swap -> action.copy(shouldShowBadge = showBadge)
                else -> action
            }
        }
        .toPersistentList()
}