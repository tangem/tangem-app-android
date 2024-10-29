package com.tangem.features.onramp.tokenlist.entity

import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
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
    val items: ImmutableList<TokensListItemUM>,
    val isBalanceHidden: Boolean,
) {

    /** Get search bar if it exists */
    fun getSearchBar(): TokensListItemUM.SearchBar? {
        return items.firstOrNull() as? TokensListItemUM.SearchBar
    }

    /** Get tokens */
    fun getTokens(): ImmutableList<TokensListItemUM> {
        if (getSearchBar() == null) return items

        return if (items.size > 1) {
            items.subList(fromIndex = 1, toIndex = items.size)
        } else {
            persistentListOf()
        }
    }
}