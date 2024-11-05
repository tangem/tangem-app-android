package com.tangem.features.onramp.selectcountry.model

import com.tangem.features.onramp.selectcountry.entity.CountryItemState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
// [REDACTED_TODO_COMMENT]
@Suppress("MagicNumber")
internal object MockedCountriesData {

    fun getCountryItems(): Flow<List<CountryItemState>> = flow {
        delay(3_000) // simulate network call
        emit(getMockedCountries())
    }
    fun getLoadingItems(): List<CountryItemState> = MutableList(5) {
        CountryItemState.Loading("Loading #$it")
    }

    private fun getMockedCountries(): List<CountryItemState> = MutableList(50) { index ->
        if (index % 5 == 0) {
            CountryItemState.Unavailable(
                id = "Country #$index",
                flagUrl = "",
                countryName = "Country $index",
            )
        } else {
            CountryItemState.Content(
                id = "Country #$index",
                flagUrl = "",
                countryName = "Country $index",
                onClick = {},
                isSelected = index == 7,
            )
        }
    }
}
