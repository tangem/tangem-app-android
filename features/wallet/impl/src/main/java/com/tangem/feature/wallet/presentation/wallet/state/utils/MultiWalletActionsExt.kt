package com.tangem.feature.wallet.presentation.wallet.state.utils

import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.TangemTheme
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
    return buttons.map { it.withEnabled(isEnabled = true) }.toPersistentList()
}

internal fun WalletUM.Content.disableButtons(): PersistentList<TangemButtonUM> {
    return buttons.map { it.withEnabled(isEnabled = false) }.toPersistentList()
}

private fun TangemButtonUM.withEnabled(isEnabled: Boolean): TangemButtonUM {
    val refreshedIcon = (tangemIconUM as? TangemIconUM.Icon)?.copy(
        tint = {
            if (isEnabled) {
                TangemTheme.colors2.graphic.neutral.primary
            } else {
                TangemTheme.colors2.graphic.neutral.quaternary
            }
        },
    ) ?: tangemIconUM
    return copy(isEnabled = isEnabled, tangemIconUM = refreshedIcon)
}

private fun WalletState.MultiCurrency.Content.changeAvailability(enabled: Boolean): PersistentList<WalletManageButton> {
    return buttons
        .map { action ->
            when (action) {
                is WalletManageButton.Buy -> action.copy(enabled = enabled)
                is WalletManageButton.AddFunds -> action.copy(enabled = enabled)
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