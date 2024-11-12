package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import kotlinx.collections.immutable.persistentListOf

/**
 * Base [SearchBarUM] transformer
 *
[REDACTED_AUTHOR]
 */
internal abstract class SearchBarUMTransformer : TokenListUMTransformer {

    abstract fun transform(prevState: SearchBarUM): SearchBarUM

    override fun transform(prevState: TokenListUM): TokenListUM {
        val searchBarItem = prevState.getSearchBar()

        return if (searchBarItem != null) {
            val updatedSearchBar = searchBarItem.copy(searchBarUM = transform(searchBarItem.searchBarUM))

            prevState.copy(
                items = persistentListOf(
                    updatedSearchBar,
                    *prevState.getTokens().toTypedArray(),
                ),
            )
        } else {
            prevState
        }
    }
}