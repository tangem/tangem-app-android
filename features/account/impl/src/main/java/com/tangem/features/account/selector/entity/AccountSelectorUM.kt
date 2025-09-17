package com.tangem.features.account.selector.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

data class AccountSelectorUM(
    val items: ImmutableList<AccountSelectorItemUM>,
    val isSingleWallet: Boolean,
)

@Immutable
sealed interface AccountSelectorItemUM {
    val id: String

    data class Wallet(
        override val id: String,
        val name: TextReference,
    ) : AccountSelectorItemUM

    data class Account(
        val account: TokenItemState,
        val isBalanceHidden: Boolean,
    ) : AccountSelectorItemUM {
        override val id: String = account.id
    }
}