package com.tangem.core.ui.components.tokenlist.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference

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
     * Network group title
     *
     * @property id   id
     * @property name network group name
     */
    @Deprecated("Use GroupTitle instead") // TODO: [REDACTED_JIRA]
    data class NetworkGroupTitle(override val id: Int, val name: TextReference) : TokensListItemUM

    /**
     * Group title
     *
     * @property id   id
     * @property text title value
     */
    data class GroupTitle(override val id: Any, val text: TextReference) : TokensListItemUM

    /**
     * Token item
     *
     * @property state token state
     */
    data class Token(val state: TokenItemState) : TokensListItemUM {
        override val id: String = state.id
    }

    data class Text(override val id: Any, val text: TextReference) : TokensListItemUM
}