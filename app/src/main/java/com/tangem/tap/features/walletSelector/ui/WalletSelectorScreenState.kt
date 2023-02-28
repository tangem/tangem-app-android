package com.tangem.tap.features.walletSelector.ui

import androidx.compose.runtime.Immutable
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.walletSelector.ui.model.DialogModel
import com.tangem.tap.features.walletSelector.ui.model.MultiCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.SingleCurrencyUserWalletItem

@Immutable
internal data class WalletSelectorScreenState(
    val multiCurrencyWallets: List<MultiCurrencyUserWalletItem> = emptyList(),
    val singleCurrencyWallets: List<SingleCurrencyUserWalletItem> = emptyList(),
    val selectedUserWalletId: UserWalletId? = null,
    val isLocked: Boolean = false,
    val editingUserWalletsIds: List<UserWalletId> = listOf(),
    val dialog: DialogModel? = null,
    val showAddCardProgress: Boolean = false,
    val showUnlockProgress: Boolean = false,
    val error: TextReference? = null,
)
