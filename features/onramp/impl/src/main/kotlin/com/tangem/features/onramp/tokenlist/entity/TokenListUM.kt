package com.tangem.features.onramp.tokenlist.entity

import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Token list UM
 *
 * @property availableItems   available items (search bar, header, tokens)
 * @property unavailableItems unavailable items (header, tokens)
 * @property isBalanceHidden  flag that indicates if balance should be hidden
 *
[REDACTED_AUTHOR]
 */
internal data class TokenListUM(
    val availableItems: ImmutableList<TokensListItemUM>,
    val unavailableItems: ImmutableList<TokensListItemUM>,
    val isBalanceHidden: Boolean,
) {

    /** Get search bar if it exists */
    fun getSearchBar(): TokensListItemUM.SearchBar? {
        return availableItems.firstOrNull() as? TokensListItemUM.SearchBar
    }

    /** Get tokens */
    fun getTokens(): ImmutableList<TokensListItemUM> {
        if (getSearchBar() == null) return availableItems

        return if (availableItems.size > 1) {
            availableItems.subList(fromIndex = 1, toIndex = availableItems.size)
        } else {
            persistentListOf()
        }
    }
}