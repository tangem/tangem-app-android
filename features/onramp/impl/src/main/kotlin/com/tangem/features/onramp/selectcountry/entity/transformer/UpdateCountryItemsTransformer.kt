package com.tangem.features.onramp.selectcountry.entity.transformer

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.selectcountry.entity.CountryItemState
import com.tangem.features.onramp.selectcountry.entity.CountryListUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class UpdateCountryItemsTransformer(
    private val maybeCountries: Either<OnrampError, List<OnrampCountry>>,
    private val defaultCountry: OnrampCountry?,
    private val query: String,
    private val onRetry: () -> Unit,
    private val onCountryClick: (OnrampCountry) -> Unit,
) : Transformer<CountryListUM> {

    override fun transform(prevState: CountryListUM): CountryListUM {
        return maybeCountries.fold(
            ifLeft = { CountryListUM.Error(searchBarUM = prevState.searchBarUM, onRetry = onRetry) },
            ifRight = { countries ->
                val countriesListItems = countries.filterByQuery().toUiModels()
                CountryListUM.Content(
                    searchBarUM = prevState.searchBarUM,
                    items = countriesListItems.toImmutableList(),
                )
            },
        )
    }

    private fun List<OnrampCountry>.toUiModels(): List<CountryItemState.WithContent> {
        return map { country ->
            if (country.onrampAvailable) {
                CountryItemState.WithContent.Content(
                    id = country.id,
                    flagUrl = country.image,
                    countryName = country.name,
                    isSelected = defaultCountry?.id?.equals(country.id) == true,
                    onClick = { onCountryClick(country) },
                )
            } else {
                CountryItemState.WithContent.Unavailable(
                    id = country.id,
                    flagUrl = country.image,
                    countryName = country.name,
                )
            }
        }
    }

    private fun List<OnrampCountry>.filterByQuery(): List<OnrampCountry> {
        return filter { country ->
            country.alpha3.lowercase().contains(query.lowercase()) ||
                country.name.lowercase().contains(query.lowercase())
        }
    }
}