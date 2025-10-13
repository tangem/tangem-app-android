package com.tangem.core.ui.components.tokenlist.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/** Tokens list item state */
@Immutable
sealed interface TokensListItemUM {

    /** Unique ID */
    val id: Any

    /**
     * Search bar item
     *
     * @property id          id
     * @property searchBarUM search bar UI model
     */
    data class SearchBar(
        override val id: Any = "search_bar",
        val searchBarUM: SearchBarUM,
    ) : TokensListItemUM

    /**
     * Group title
     *
     * @property id   id
     * @property text title value
     */
    data class GroupTitle(override val id: Any, val text: TextReference) : TokensListItemUM, PortfolioTokensListItemUM

    /**
     * Token item
     *
     * @property state token state
     */
    data class Token(val state: TokenItemState) : TokensListItemUM, PortfolioTokensListItemUM {
        override val id: String = state.id
    }

    data class Portfolio(
        val tokenItemUM: TokenItemState,
        val isExpanded: Boolean,
        val isCollapsable: Boolean,
        val tokens: ImmutableList<PortfolioTokensListItemUM>,
    ) : TokensListItemUM {
        override val id: String = tokenItemUM.id
    }

    data class Text(override val id: Any, val text: TextReference) : TokensListItemUM
}

sealed interface PortfolioTokensListItemUM {
    /** Unique ID */
    val id: Any
}