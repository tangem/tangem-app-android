package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.feature.wallet.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed class WalletTokensListState {

    data object Empty : WalletTokensListState()

    sealed class ContentState : WalletTokensListState() {

        abstract val items: ImmutableList<TokensListItemUM>
        abstract val organizeTokensButtonConfig: OrganizeTokensButtonConfig?

        data object Loading : ContentState() {
            override val items = persistentListOf<TokensListItemUM>()
            override val organizeTokensButtonConfig = null
        }

        data class Content(
            override val items: ImmutableList<TokensListItemUM>,
            override val organizeTokensButtonConfig: OrganizeTokensButtonConfig?,
        ) : ContentState()

        data object Locked : ContentState() {
            override val items = persistentListOf(
                TokensListItemUM.GroupTitle(
                    id = 42,
                    text = resourceReference(
                        id = R.string.wallet_network_group_title,
                        formatArgs = wrappedList(resourceReference(id = R.string.main_tokens)),
                    ),
                ),
                TokensListItemUM.Token(state = TokenItemState.Locked(id = "Locked#1")),
            )
            override val organizeTokensButtonConfig = null
        }
    }

    data class OrganizeTokensButtonConfig(val isEnabled: Boolean, val onClick: () -> Unit)
}