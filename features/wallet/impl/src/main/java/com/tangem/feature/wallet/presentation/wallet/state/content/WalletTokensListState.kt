package com.tangem.feature.wallet.presentation.wallet.state.content

import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Wallet tokens list state
 *
 * @property items                  content items
 * @property onOrganizeTokensClick  lambda be invoked when organize tokens button is clicked
 *
 * @author Andrew Khokhlov on 28/07/2023
 */
// TODO: Finalize strings https://tangem.atlassian.net/browse/AND-4040
internal sealed class WalletTokensListState(
    open val items: ImmutableList<TokensListItemState>,
    open val onOrganizeTokensClick: (() -> Unit)?,
) {

    /**
     * Content state
     *
     * @property items                  content items
     * @property onOrganizeTokensClick  lambda be invoked when organize tokens button is clicked
     */
    data class Content(
        override val items: ImmutableList<TokensListItemState>,
        override val onOrganizeTokensClick: (() -> Unit)?,
    ) : WalletTokensListState(items, onOrganizeTokensClick)

    /** Locked content state */
    object Locked :
        WalletTokensListState(
            items = persistentListOf(
                TokensListItemState.NetworkGroupTitle(networkName = "Tokens"),
                TokensListItemState.Token(state = TokenItemState.Loading),
            ),
            onOrganizeTokensClick = null,
        ),
        WalletLockedContentState

    /** Tokens list item state */
    sealed interface TokensListItemState {

        /**
         * Network group title item
         *
         * @property networkName network name
         */
        data class NetworkGroupTitle(val networkName: String) : TokensListItemState

        /**
         * Token item
         *
         * @property state token item state
         */
        data class Token(val state: TokenItemState) : TokensListItemState
    }
}
