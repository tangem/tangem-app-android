package com.tangem.features.details.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class UserWalletListUM(
    val userWallets: ImmutableList<UserWalletItemUM>,
    val isWalletSavingInProgress: Boolean,
    val addNewWalletText: TextReference,
    val onAddNewWalletClick: () -> Unit,
    val walletReorderUM: WalletReorderUM,
)

internal data class WalletReorderUM(
    val isDragEnabled: Boolean,
    val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    val onDragStopped: () -> Unit,
)