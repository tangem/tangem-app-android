package com.tangem.data.onramp.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.grow.datasource.onramp.models.response.model.OnrampCountryDTO
import com.tangem.grow.datasource.onramp.models.response.model.OnrampCurrencyDTO
import org.junit.jupiter.api.Test

internal class CountryConverterTest {

    private val converter = CountryConverter(CurrencyConverter())

    @Test
    fun `GIVEN country dto WHEN convert THEN generated id and nested currency are mapped`() {
        // Act
        val actual = converter.convert(createDto())

        // Assert
        assertThat(actual).isEqualTo(createCountry())
    }

    @Test
    fun `GIVEN country WHEN convertBack THEN id is not persisted and dto fields are mapped`() {
        // Act
        val actual = converter.convertBack(createCountry())

        // Assert
        assertThat(actual).isEqualTo(createDto())
    }

    private fun createDto() = OnrampCountryDTO(
        name = "United States",
        code = "US",
        image = "https://example.com/us.png",
        alpha3 = "USA",
        continent = "North America",
        defaultCurrency = OnrampCurrencyDTO("US Dollar", "USD", null, 2, "USD"),
        onrampAvailable = true,
    )

    private fun createCountry() = OnrampCountry(
        id = "USA-United States",
        name = "United States",
        code = "US",
        image = "https://example.com/us.png",
        alpha3 = "USA",
        continent = "North America",
        defaultCurrency = OnrampCurrency("US Dollar", "USD", null, 2, "USD"),
        isOnrampAvailable = true,
    )
}