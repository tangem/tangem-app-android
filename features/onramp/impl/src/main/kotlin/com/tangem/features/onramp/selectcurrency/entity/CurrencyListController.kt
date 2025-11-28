package com.tangem.features.onramp.selectcurrency.entity

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.features.onramp.utils.SearchBarUMTransformer
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class CurrencyListController(
    currencySearchBarUM: SearchBarUM,
    loadingSections: ImmutableList<CurrenciesSection<CurrencyItemState.Loading>>,
) {
    val state: StateFlow<CurrenciesListUM>
        field = MutableStateFlow<CurrenciesListUM>(
            value = CurrenciesListUM.Loading(
                searchBarUM = currencySearchBarUM,
                sections = loadingSections,
            ),
        )

    fun update(transformer: Transformer<CurrenciesListUM>) {
        state.update(transformer::transform)
    }

    fun update(transformer: SearchBarUMTransformer) {
        state.update { prevState ->
            val searchBarUM = transformer.transform(prevState.searchBarUM)
            prevState.copySealed(searchBarUM)
        }
    }
}