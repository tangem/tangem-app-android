package com.tangem.features.onramp.selectcountry.entity.transformer

import arrow.core.Either
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selectcountry.entity.CountriesListItemUM
import com.tangem.features.onramp.selectcountry.entity.CountryItemState
import com.tangem.features.onramp.selectcountry.entity.CountryListUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class UpdateCountryItemsTransformer(
    private val maybeCountries: Either<Throwable, List<OnrampCountry>>,
    private val defaultCountry: OnrampCountry?,
    private val query: String,
    private val onQueryChange: (String) -> Unit,
    private val onActiveChange: (Boolean) -> Unit,
    private val onCountryClick: (OnrampCountry) -> Unit,
) : Transformer<CountryListUM> {

    override fun transform(prevState: CountryListUM): CountryListUM {
        val searchBarItem = prevState.getSearchBar() ?: createSearchBarItem()
        return maybeCountries.fold(
            ifLeft = { prevState }, // TODO: [REDACTED_JIRA]
            ifRight = { countries ->
                val countriesListItems = countries.filterByQuery().toUiModels()
                prevState.copy(items = (listOf(searchBarItem) + countriesListItems).toImmutableList())
            },
        )
    }

    private fun List<OnrampCountry>.toUiModels(): List<CountriesListItemUM.Country> {
        return map { country ->
            val countryItemState = if (country.onrampAvailable) {
                CountryItemState.Content(
                    id = country.id,
                    flagUrl = country.image,
                    countryName = country.name,
                    isSelected = defaultCountry?.id?.equals(country.id) == true,
                    onClick = { onCountryClick(country) },
                )
            } else {
                CountryItemState.Unavailable(id = country.id, flagUrl = country.image, countryName = country.name)
            }
            CountriesListItemUM.Country(countryItemState)
        }
    }

    private fun List<OnrampCountry>.filterByQuery(): List<OnrampCountry> {
        return filter { country ->
            country.alpha3.lowercase().contains(query.lowercase()) ||
                country.name.lowercase().contains(query.lowercase())
        }
    }

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