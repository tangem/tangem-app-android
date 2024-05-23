package com.tangem.features.details.component

import androidx.annotation.DrawableRes
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

interface UserWalletListComponent {

    val state: StateFlow<State>

    data class State(
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

    interface Factory {
        fun create(context: AppComponentContext): UserWalletListComponent
    }
}
