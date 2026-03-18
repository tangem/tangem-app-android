package com.tangem.feature.wallet.presentation.wallet.state.utils

import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal fun WalletState.MultiCurrency.Content.enableButtons(): PersistentList<WalletManageButton> {
    return changeAvailability(enabled = true)
}

internal fun WalletState.MultiCurrency.Content.disableButtons(): PersistentList<WalletManageButton> {
    return changeAvailability(enabled = false)
}

internal fun WalletUM.Content.enableButtons(): PersistentList<TangemButtonUM> {
    return buttons.map { it.copy(isEnabled = true) }.toPersistentList()
}

internal fun WalletUM.Content.disableButtons(): PersistentList<TangemButtonUM> {
    return buttons.map { it.copy(isEnabled = false) }.toPersistentList()
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