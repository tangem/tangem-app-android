package com.tangem.features.onramp.selectcountry.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.fields.entity.SearchBarUM

@Immutable
internal sealed interface CountriesListItemUM {
    /** Unique ID */
    val id: Any

    data class SearchBar(
        override val id: Any = "search_bar",
        val searchBarUM: SearchBarUM,
    ) : CountriesListItemUM

    data class Country(val state: CountryItemState) : CountriesListItemUM {
        override val id = state.id
    }
}
