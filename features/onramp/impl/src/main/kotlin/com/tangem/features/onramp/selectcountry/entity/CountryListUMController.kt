package com.tangem.features.onramp.selectcountry.entity

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.features.onramp.utils.SearchBarUMTransformer
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class CountryListUMController(
    searchBarUM: SearchBarUM,
    loadingItems: ImmutableList<CountryItemState.Loading>,
) {

    val state: StateFlow<CountryListUM> get() = _state.asStateFlow()
    private val _state: MutableStateFlow<CountryListUM> =
        MutableStateFlow(value = CountryListUM.Loading(searchBarUM = searchBarUM, items = loadingItems))

    fun update(transformer: Transformer<CountryListUM>) {
        _state.update(transformer::transform)
    }

    fun update(transformer: SearchBarUMTransformer) {
        _state.update { prevState ->
            val searchBarUM = transformer.transform(prevState.searchBarUM)
            prevState.copySealed(searchBarUM)
        }
    }
}