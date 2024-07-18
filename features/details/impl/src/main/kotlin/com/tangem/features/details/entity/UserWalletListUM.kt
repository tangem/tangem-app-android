package com.tangem.features.details.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class UserWalletListUM(
    val userWallets: ImmutableList<UserWalletUM>,
    val isWalletSavingInProgress: Boolean,
    val addNewWalletText: TextReference,
    val onAddNewWalletClick: () -> Unit,
) {

    @Immutable
    data class UserWalletUM(
        val id: UserWalletId,
        val name: TextReference,
        val information: TextReference,
        val imageUrl: String,
        val isEnabled: Boolean,
        val onClick: () -> Unit,
    )
}
