package com.tangem.features.details.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class UserWalletListUM(
    val userWallets: ImmutableList<UserWalletItemUM>,
    val isWalletSavingInProgress: Boolean,
    val addNewWalletText: TextReference,
    @DrawableRes val addNewWalletIconRes: Int?,
    val onAddNewWalletClick: () -> Unit,
)