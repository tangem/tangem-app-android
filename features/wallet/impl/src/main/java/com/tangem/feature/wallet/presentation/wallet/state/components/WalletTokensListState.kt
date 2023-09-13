package com.tangem.feature.wallet.presentation.wallet.state.components

import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import javax.annotation.concurrent.Immutable

/**
 * Wallet tokens list state
 *
[REDACTED_AUTHOR]
 */
internal sealed class WalletTokensListState {

    /** Empty token list state */
    object Empty : WalletTokensListState()

    /**
     * Wallet content token list state
     *
     * @property items                  content items
     */
    sealed class ContentState(
        open val items: ImmutableList<TokensListItemState>,
        open val organizeTokensButton: OrganizeTokensButtonState,
    ) : WalletTokensListState()

    /**
     * Loading content state
     *
     * @property items content items
     */
    data class Loading(
        override val items: ImmutableList<TokensListItemState.Token> = persistentListOf(
            TokensListItemState.Token(state = TokenItemState.Loading(id = FIRST_LOADING_TOKEN_ID)),
            TokensListItemState.Token(state = TokenItemState.Loading(id = SECOND_LOADING_TOKEN_ID)),
        ),
    ) : ContentState(items = items, organizeTokensButton = OrganizeTokensButtonState.Hidden)

    /**
     * Content state
     *
     * @property items                 content items
     * @property organizeTokensButton  represents the state of the 'Organize Tokens' button
     */
    data class Content(
        override val items: ImmutableList<TokensListItemState>,
        override val organizeTokensButton: OrganizeTokensButtonState,
    ) : ContentState(items, organizeTokensButton)

    /** Locked content state */
    object Locked : ContentState(
        items = persistentListOf(
            TokensListItemState.NetworkGroupTitle(id = 42, name = TextReference.Res(id = R.string.main_tokens)),
            TokensListItemState.Token(state = TokenItemState.Locked(id = LOCKED_TOKEN_ID)),
        ),
        organizeTokensButton = OrganizeTokensButtonState.Hidden,
    )

    /**
     * Represents the state of the 'Organize Tokens' button.
     */
    @Immutable
    sealed class OrganizeTokensButtonState {

        /** Represents the state where the 'Organize Tokens' button is hidden. */
        object Hidden : OrganizeTokensButtonState()

        /**
         * Represents the state where the 'Organize Tokens' button is visible.
         *
         * @property isEnabled Indicates if the button is enabled or not.
         * @property onClick Callback to be executed when the button is clicked.
         */
        data class Visible(
            val isEnabled: Boolean,
            val onClick: () -> Unit,
        ) : OrganizeTokensButtonState()
    }

    /** Tokens list item state */
    @Immutable
    sealed interface TokensListItemState {

        val id: Any

        /**
         * Network group title item
         *
         * @property name network name
         */
        data class NetworkGroupTitle(
            override val id: Int,
            val name: TextReference,
        ) : TokensListItemState

        /**
         * Token item
         *
         * @property state token item state
         */
        data class Token(val state: TokenItemState) : TokensListItemState {
            override val id: String = state.id
        }
    }

    private companion object {
        const val FIRST_LOADING_TOKEN_ID = "Loading#1"
        const val SECOND_LOADING_TOKEN_ID = "Loading#2"
        const val LOCKED_TOKEN_ID = "Locked#1"
    }
}