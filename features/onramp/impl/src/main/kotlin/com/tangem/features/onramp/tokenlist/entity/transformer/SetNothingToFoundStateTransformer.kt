package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class SetNothingToFoundStateTransformer(
    private val isBalanceHidden: Boolean,
    private val hasSearchBar: Boolean,
    private val emptySearchMessageReference: TextReference,
    private val onQueryChange: (String) -> Unit,
    private val onActiveChange: (Boolean) -> Unit,
) : TokenListUMTransformer {

    override fun transform(prevState: TokenListUM): TokenListUM {
        val searchBarItem = if (hasSearchBar) {
            prevState.getSearchBar() ?: createSearchBarItem()
        } else {
            null
        }

        return prevState.copy(
            availableItems = buildList {
                if (searchBarItem != null) {
                    add(searchBarItem)
                }

                createGroupTitle(
                    textReference = resourceReference(id = R.string.exchange_tokens_available_tokens_header),
                )
                    .let(::add)

                TokensListItemUM.Text(
                    id = emptySearchMessageReference.hashCode(),
                    text = emptySearchMessageReference,
                ).let(::add)
            }
                .toImmutableList(),
            unavailableItems = persistentListOf(),
            isBalanceHidden = isBalanceHidden,
        )
    }

    private fun createSearchBarItem(): TokensListItemUM.SearchBar {
        return TokensListItemUM.SearchBar(
            searchBarUM = SearchBarUM(
                placeholderText = resourceReference(id = R.string.common_search),
                query = "",
                onQueryChange = onQueryChange,
                isActive = false,
                onActiveChange = onActiveChange,
            ),
        )
    }

    private fun createGroupTitle(textReference: TextReference): TokensListItemUM.GroupTitle {
        return TokensListItemUM.GroupTitle(id = textReference.hashCode(), text = textReference)
    }
}
