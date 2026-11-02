package com.tangem.data.onramp.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.grow.datasource.onramp.models.response.model.OnrampCurrencyDTO
import org.junit.jupiter.api.Test

internal class CurrencyConverterTest {

    private val converter = CurrencyConverter()

    @Test
    fun `GIVEN dto without unit WHEN convert THEN currency code is used as unit`() {
        // Act
        val actual = converter.convert(createDto(unit = null))

        // Assert
        assertThat(actual).isEqualTo(createCurrency(unit = "USD"))
    }

    @Test
    fun `GIVEN currency WHEN convertBack THEN all fields are mapped`() {
        // Act
        val actual = converter.convertBack(createCurrency(unit = "$") )

        // Assert
        assertThat(actual).isEqualTo(createDto(unit = "$") )
    }

    private fun createDto(unit: String?) = OnrampCurrencyDTO(
        name = "US Dollar",
        code = "USD",
        image = "https://example.com/usd.png",
        precision = 2,
        unit = unit,
    )

    private fun createCurrency(unit: String) = OnrampCurrency(
        name = "US Dollar",
        code = "USD",
        image = "https://example.com/usd.png",
        precision = 2,
        unit = unit,
    )
}