package com.tangem.features.onramp.selectcountry.entity

import com.tangem.features.onramp.selectcountry.entity.transformer.UpdateCountryItemsTransformer
import com.tangem.features.onramp.selectcountry.model.MockedCountriesData
import com.tangem.features.onramp.utils.SearchBarUMTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

internal class CountryListUMController @Inject constructor() {

    val state: StateFlow<CountryListUM> get() = _state.asStateFlow()
    private val _state: MutableStateFlow<CountryListUM> = MutableStateFlow(
        value = CountryListUM(
            items = MockedCountriesData.getLoadingItems().map(CountriesListItemUM::Country).toImmutableList(),
        ),
    )

    fun update(transformer: UpdateCountryItemsTransformer) {
        Timber.d("Applying ${transformer::class.simpleName}")
        _state.update(transformer::transform)
    }

    fun update(transformer: SearchBarUMTransformer) {
        Timber.d("Applying ${transformer::class.simpleName}")
        _state.update { prevState ->
            val searchBarItem = prevState.getSearchBar()

            if (searchBarItem != null) {
                val updatedSearchBar =
                    searchBarItem.copy(searchBarUM = transformer.transform(searchBarItem.searchBarUM))

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

    /** Get search bar if it exists */
    fun getSearchBar(): CountriesListItemUM.SearchBar? {
        return _state.value.getSearchBar()
    }
}
