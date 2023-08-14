package com.tangem.feature.wallet.presentation.wallet.state.components

import com.tangem.core.ui.components.wallet.WalletLockedContentState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Wallet tokens list state
 *
 * @property items                  content items
 * @property onOrganizeTokensClick  lambda be invoked when organize tokens button is clicked
 *
* [REDACTED_AUTHOR]
 */
internal sealed class WalletTokensListState(
    open val items: ImmutableList<TokensListItemState>,
    open val onOrganizeTokensClick: (() -> Unit)?,
) {

    /** Loading content state */
    object Loading : WalletTokensListState(
        items = persistentListOf(
            TokensListItemState.Token(state = TokenItemState.Loading(id = FIRST_LOADING_TOKEN_ID)),
            TokensListItemState.Token(state = TokenItemState.Loading(id = SECOND_LOADING_TOKEN_ID)),
        ),
        onOrganizeTokensClick = null,
    )

    /**
     * Content state
     *
     * @property items                 content items
     * @property onOrganizeTokensClick lambda be invoked when organize tokens button is clicked
     */
    data class Content(
        override val items: ImmutableList<TokensListItemState>,
        override val onOrganizeTokensClick: (() -> Unit)?,
    ) : WalletTokensListState(items, onOrganizeTokensClick)

    /** Locked content state */
    object Locked :
        WalletTokensListState(
            items = persistentListOf(
                TokensListItemState.NetworkGroupTitle(value = TextReference.Res(id = R.string.main_tokens)),
                TokensListItemState.Token(state = TokenItemState.Loading(id = LOCKED_TOKEN_ID)),
            ),
            onOrganizeTokensClick = null,
        ),
        WalletLockedContentState

    /** Tokens list item state */
    sealed interface TokensListItemState {

        /**
         * Network group title item
         *
         * @property value network name
         */
        data class NetworkGroupTitle(val value: TextReference) : TokensListItemState

        /**
         * Token item
         *
         * @property state token item state
         */
        data class Token(val state: TokenItemState) : TokensListItemState
    }

    private companion object {
        const val FIRST_LOADING_TOKEN_ID = "Loading#1"
        const val SECOND_LOADING_TOKEN_ID = "Loading#2"
        const val LOCKED_TOKEN_ID = "Locked#1"
    }
}
