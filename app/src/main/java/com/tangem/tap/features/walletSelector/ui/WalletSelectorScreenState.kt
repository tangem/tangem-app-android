package com.tangem.tap.features.walletSelector.ui

import androidx.compose.runtime.Immutable
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.walletSelector.ui.model.MultiCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.RenameWalletDialog
import com.tangem.tap.features.walletSelector.ui.model.SingleCurrencyUserWalletItem

@Immutable
internal data class WalletSelectorScreenState(
    val multiCurrencyWallets: List<MultiCurrencyUserWalletItem> = emptyList(),
    val singleCurrencyWallets: List<SingleCurrencyUserWalletItem> = emptyList(),
    val selectedWalletId: String? = null,
    val isLocked: Boolean = false,
    val editingWalletsIds: List<String> = listOf(),
    val renameWalletDialog: RenameWalletDialog? = null,
    val showAddCardProgress: Boolean = false,
    val showUnlockProgress: Boolean = false,
    val error: TextReference? = null,
)
