package com.tangem.features.feed.model.search.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.model.search.state.transformers.SearchUMTransformer
import com.tangem.features.feed.ui.search.state.SearchContentUM
import com.tangem.features.feed.ui.search.state.SearchUM
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class SearchStateController @Inject constructor() {

    private val mutableUiState: MutableStateFlow<SearchUM> = MutableStateFlow(value = getInitialState())

    val uiState: StateFlow<SearchUM> = mutableUiState.asStateFlow()

    val value: SearchUM
        get() = uiState.value

    fun update(transformer: SearchUMTransformer) {
        mutableUiState.update(function = transformer::transform)
    }

    private fun getInitialState(): SearchUM {
        return SearchUM(
            searchBar = SearchBarUM(
                placeholderText = resourceReference(id = R.string.markets_search_title_placeholder),
                query = "",
                onQueryChange = {},
                isActive = false,
                onActiveChange = {},
                onClearClick = {},
            ),
            content = SearchContentUM.InitialEmpty,
        )
    }
}