package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.wallet.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed class WalletTokensListState {

    data object Empty : WalletTokensListState()

    sealed class ContentState : WalletTokensListState() {

        abstract val items: ImmutableList<TokensListItemState>
        abstract val organizeTokensButtonConfig: OrganizeTokensButtonConfig?

        data object Loading : ContentState() {
            override val items = persistentListOf<TokensListItemState>()
            override val organizeTokensButtonConfig = null
        }

        data class Content(
            override val items: ImmutableList<TokensListItemState>,
            override val organizeTokensButtonConfig: OrganizeTokensButtonConfig?,
        ) : ContentState()

        data object Locked : ContentState() {
            override val items = persistentListOf(
                TokensListItemState.NetworkGroupTitle(id = 42, name = TextReference.Res(id = R.string.main_tokens)),
                TokensListItemState.Token(state = TokenItemState.Locked(id = "Locked#1")),
            )
            override val organizeTokensButtonConfig = null
        }
    }

    data class OrganizeTokensButtonConfig(val isEnabled: Boolean, val onClick: () -> Unit)

    @Immutable
    sealed class TokensListItemState {

        abstract val id: Any

        data class SearchBar(
            override val id: Any = "search_bar",
            val searchBarUM: SearchBarUM,
        ) : TokensListItemState()

        data class NetworkGroupTitle(override val id: Int, val name: TextReference) : TokensListItemState()

        data class Token(val state: TokenItemState) : TokensListItemState() {
            override val id: String = state.id
        }
    }
}