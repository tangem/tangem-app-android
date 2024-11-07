package com.tangem.features.onramp.selectcountry.entity

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class CountryListUM(val items: ImmutableList<CountriesListItemUM>) {

    /** Get search bar if it exists */
    fun getSearchBar(): CountriesListItemUM.SearchBar? {
        return items.firstOrNull() as? CountriesListItemUM.SearchBar
    }

    /** Get tokens */
    fun getCountries(): ImmutableList<CountriesListItemUM> {
        if (getSearchBar() == null) return items

        return if (items.size > 1) {
            items.subList(fromIndex = 1, toIndex = items.size)
        } else {
            persistentListOf()
        }
    }
}