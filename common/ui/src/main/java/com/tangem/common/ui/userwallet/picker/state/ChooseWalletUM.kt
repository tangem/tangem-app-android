package com.tangem.common.ui.userwallet.picker.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/** Wallet picker sheet */
@Immutable
data class ChooseWalletUM(
    val wallets: ImmutableList<WalletItemUM>,
    val onDismiss: () -> Unit,
) {

    data class WalletItemUM(
        val id: String,
        val name: TextReference,
        val image: UserWalletItemUM.ImageState,
        val info: TextReference,
        val isSelected: Boolean,
        val onClick: () -> Unit,
    )
}