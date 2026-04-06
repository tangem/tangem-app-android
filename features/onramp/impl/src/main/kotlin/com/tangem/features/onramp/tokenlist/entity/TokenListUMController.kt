package com.tangem.features.onramp.tokenlist.entity

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.utils.SearchBarUMTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * [TokenListUM] controller
 *
[REDACTED_AUTHOR]
 */
internal class TokenListUMController @Inject constructor() {

    val state: StateFlow<TokenListUM>
        field = MutableStateFlow(
            value = TokenListUM(
                searchBarUM = SearchBarUM(
                    placeholderText = resourceReference(id = R.string.common_search),
                    query = "",
                    onQueryChange = {},
                    isActive = false,
                    onActiveChange = {},
                ),
                availableItems = persistentListOf(),
                unavailableItems = persistentListOf(),
                tokensListData = TokenListUMData.EmptyList,
                isBalanceHidden = false,
            ),
        )

    fun update(transform: (TokenListUM) -> TokenListUM) {
        TangemLogger.d("Applying non-name transformation")
        state.update(transform)
    }

    fun update(transformer: TokenListUMTransformer) {
        TangemLogger.d("Applying ${transformer::class.simpleName ?: "unknown"}")
        state.update(transformer::transform)
    }

    fun update(transformer: SearchBarUMTransformer) {
        TangemLogger.d("Applying ${transformer::class.simpleName ?: "unknown"}")
        state.update { prevState ->
            prevState.copy(
                searchBarUM = transformer.transform(prevState.searchBarUM),
            )
        }
    }
}