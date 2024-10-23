package com.tangem.feature.wallet.presentation.tokenlist.entity

import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.TokensListItemState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Token list UM
 *
 * @property items           items (search bar, tokens, headers)
 * @property isBalanceHidden flag that indicates if balance should be hidden
 *
[REDACTED_AUTHOR]
 */
internal data class TokenListUM(
    val items: ImmutableList<TokensListItemState>,
    val isBalanceHidden: Boolean,
) {

    /** Get search bar if it exists */
    fun getSearchBar(): TokensListItemState.SearchBar? {
        return items.firstOrNull() as? TokensListItemState.SearchBar
    }

    /** Get tokens */
    fun getTokens(): ImmutableList<TokensListItemState> {
        if (getSearchBar() == null) return items

        return if (items.size > 1) {
            items.subList(fromIndex = 1, toIndex = items.size)
        } else {
            persistentListOf()
        }
    }
}