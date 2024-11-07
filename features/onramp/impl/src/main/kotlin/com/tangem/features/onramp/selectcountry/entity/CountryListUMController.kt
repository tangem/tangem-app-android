package com.tangem.features.onramp.selectcountry.entity

import com.tangem.features.onramp.selectcountry.entity.transformer.UpdateCountryItemsTransformer
import com.tangem.features.onramp.utils.SearchBarUMTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val LOADING_ITEMS_COUNT = 5

internal class CountryListUMController @Inject constructor() {

    val state: StateFlow<CountryListUM> get() = _state.asStateFlow()
    private val _state: MutableStateFlow<CountryListUM> = MutableStateFlow(
        value = CountryListUM(
            items = getLoadingItems().map(CountriesListItemUM::Country).toImmutableList(),
        ),
    )

    fun update(transformer: UpdateCountryItemsTransformer) {
        _state.update(transformer::transform)
    }

    fun update(transformer: SearchBarUMTransformer) {
        _state.update { prevState ->
            val searchBarItem = prevState.getSearchBar()

            if (searchBarItem != null) {
                val updatedSearchBar =
                    searchBarItem.copy(searchBarUM = transformer.transform(searchBarItem.searchBarUM))

                prevState.copy(
                    items = persistentListOf(
                        updatedSearchBar,
                        *prevState.getCountries().toTypedArray(),
                    ),
                )
            } else {
                prevState
            }
        }
    }

    /** Get search bar if it exists */
    fun getSearchBar(): CountriesListItemUM.SearchBar? {
        return _state.value.getSearchBar()
    }

    private fun getLoadingItems(): List<CountryItemState> =
        MutableList(LOADING_ITEMS_COUNT) { CountryItemState.Loading("Loading #$it") }
}