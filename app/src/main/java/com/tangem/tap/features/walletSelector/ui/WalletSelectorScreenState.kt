package com.tangem.tap.features.walletSelector.ui

import androidx.compose.runtime.Immutable
import com.tangem.common.core.TangemError
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.features.walletSelector.ui.model.RenameWalletDialog
import com.tangem.tap.features.walletSelector.ui.model.UserWalletItem

@Immutable
internal data class WalletSelectorScreenState(
    val wallets: List<UserWalletItem> = listOf(),
    val selectedWalletId: String? = null,
    val fiatCurrency: FiatCurrency = FiatCurrency.Default,
    val isLocked: Boolean = true,
    val editingWalletsIds: List<String> = listOf(),
    val renameWalletDialog: RenameWalletDialog? = null,
    val showAddCardProgress: Boolean = false,
    val showUnlockProgress: Boolean = false,
    val error: TangemError? = null,
)