package com.tangem.features.details.entity

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.collections.immutable.ImmutableList

internal data class UserWalletList(
    val userWallets: ImmutableList<UserWallet>,
    val addNewWalletText: TextReference,
    val onAddNewWalletClick: () -> Unit,
) {

    data class UserWallet(
        val id: UserWalletId,
        val name: String,
        val information: TextReference,
        @DrawableRes
        val imageResId: Int,
        val onClick: () -> Unit,
    )
}
