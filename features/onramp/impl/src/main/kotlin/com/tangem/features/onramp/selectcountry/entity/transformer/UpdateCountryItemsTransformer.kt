package com.tangem.features.onramp.selectcountry.entity.transformer

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selectcountry.entity.CountriesListItemUM
import com.tangem.features.onramp.selectcountry.entity.CountryItemState
import com.tangem.features.onramp.selectcountry.entity.CountryListUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class UpdateCountryItemsTransformer(
    private val countries: List<CountryItemState>,
    private val onQueryChange: (String) -> Unit,
    private val onActiveChange: (Boolean) -> Unit,
) : Transformer<CountryListUM> {

    override fun transform(prevState: CountryListUM): CountryListUM {
        val searchBarItem = prevState.getSearchBar() ?: createSearchBarItem()

        return prevState.copy(
            items = (listOfNotNull(searchBarItem) + countries.map(::convertCountry)).toImmutableList(),
        )
    }

    // TODO: Temporarily. Will be refactored after implement domain
    private fun convertCountry(state: CountryItemState): CountriesListItemUM.Country =
        CountriesListItemUM.Country(state)

    private fun createSearchBarItem(): CountriesListItemUM.SearchBar {
        return CountriesListItemUM.SearchBar(
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
