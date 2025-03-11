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
                is WalletManageButton.Buy -> action.copy(enabled = enabled)
                is WalletManageButton.Sell -> action.copy(enabled = enabled)
                is WalletManageButton.Swap -> action.copy(enabled = enabled)
                else -> action
            }
        }
        .toPersistentList()
}

internal fun WalletState.MultiCurrency.Content.showSwapBadge(showBadge: Boolean): PersistentList<WalletManageButton> {
    return buttons
        .map { action ->
            when (action) {
                is WalletManageButton.Swap -> action.copy(showBadge = showBadge)
                else -> action
            }
        }
        .toPersistentList()
}