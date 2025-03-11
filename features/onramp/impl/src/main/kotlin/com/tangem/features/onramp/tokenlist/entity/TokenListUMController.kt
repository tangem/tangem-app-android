package com.tangem.features.onramp.tokenlist.entity

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.utils.SearchBarUMTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

/**
 * [TokenListUM] controller
 *
[REDACTED_AUTHOR]
 */
internal class TokenListUMController @Inject constructor() {

    val state: StateFlow<TokenListUM> get() = _state

    private val _state: MutableStateFlow<TokenListUM> = MutableStateFlow(
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
            isBalanceHidden = false,
        ),
    )

    fun update(transform: (TokenListUM) -> TokenListUM) {
        Timber.d("Applying non-name transformation")
        _state.update(transform)
    }

    fun update(transformer: TokenListUMTransformer) {
        Timber.d("Applying ${transformer::class.simpleName}")
        _state.update(transformer::transform)
    }

    fun update(transformer: SearchBarUMTransformer) {
        Timber.d("Applying ${transformer::class.simpleName}")
        _state.update { prevState ->
            prevState.copy(
                searchBarUM = transformer.transform(prevState.searchBarUM),
            )
        }
    }
}