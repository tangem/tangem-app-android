package com.tangem.feature.wallet.presentation.tokenlist.entity.transformer

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.tokenlist.entity.TokenListUM
import com.tangem.feature.wallet.presentation.tokenlist.entity.TokenListUMTransformer
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState.TokensListItemState
import kotlinx.collections.immutable.toImmutableList

internal class UpdateTokenItemsTransformer(
    private val tokenItemStateConverter: TokenItemStateConverter,
    private val statuses: List<CryptoCurrencyStatus>,
    private val isBalanceHidden: Boolean,
    private val hasSearchBar: Boolean,
    private val onQueryChange: (String) -> Unit,
    private val onActiveChange: (Boolean) -> Unit,
) : TokenListUMTransformer {

    override fun transform(prevState: TokenListUM): TokenListUM {
        val items = tokenItemStateConverter.convertList(input = statuses).map(TokensListItemState::Token)

        val searchBarItem = if (hasSearchBar) {
            prevState.getSearchBar() ?: createSearchBarItem()
        } else {
            null
        }

        return prevState.copy(
            items = (listOfNotNull(searchBarItem) + items).toImmutableList(),
            isBalanceHidden = isBalanceHidden,
        )
    }

    private fun createSearchBarItem(): TokensListItemState.SearchBar {
        return TokensListItemState.SearchBar(
            searchBarUM = SearchBarUM(
                placeholderText = resourceReference(id = R.string.common_search),
                query = "",
                onQueryChange = onQueryChange,
                isActive = false,
                onActiveChange = onActiveChange,
            ),
        )
    }
}