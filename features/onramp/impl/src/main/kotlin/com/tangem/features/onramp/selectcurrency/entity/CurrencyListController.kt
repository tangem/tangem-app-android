package com.tangem.features.onramp.selectcurrency.entity

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.onramp.selectcurrency.entity.transformer.UpdateCurrencyItemsTransformer
import com.tangem.features.onramp.utils.SearchBarUMTransformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class CurrencyListController(private val currencySearchBarUM: SearchBarUM) {
    val state: StateFlow<CurrenciesListUM> get() = _state.asStateFlow()
    private val _state: MutableStateFlow<CurrenciesListUM> = MutableStateFlow(value = getInitialState())

    fun update(transformer: UpdateCurrencyItemsTransformer) {
        _state.update(transformer::transform)
    }

    fun update(transformer: SearchBarUMTransformer) {
        _state.update { prevState ->
            val searchBarUM = transformer.transform(prevState.searchBarUM)
            prevState.copySealed(searchBarUM)
        }
    }

    private fun getInitialState(): CurrenciesListUM {
        return CurrenciesListUM.Loading(
            searchBarUM = currencySearchBarUM,
            sections = listOf(
                CurrenciesSection(stringReference("Popular fiats"), items = createLoadingItems("popular")),
                CurrenciesSection(stringReference("Other currencies"), items = createLoadingItems("other")),
            ).toImmutableList(),
        )
    }

    private fun createLoadingItems(prefix: String, size: Int = 5): ImmutableList<CurrencyItemState.Loading> =
        MutableList(size) { index -> CurrencyItemState.Loading("$prefix#$index") }.toImmutableList()
}
