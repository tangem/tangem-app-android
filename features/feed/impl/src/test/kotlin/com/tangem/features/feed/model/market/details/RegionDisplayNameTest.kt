package com.tangem.features.feed.model.market.details

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.util.Locale

internal class RegionDisplayNameTest {

    @Test
    fun `GIVEN a known code WHEN resolved THEN it reads in the app language`() {
        // Act
        val actual = RegionDisplayName.resolve(countryCode = "GB", locale = Locale.ENGLISH)

        // Assert
        assertThat(actual).isEqualTo("United Kingdom")
    }

    @Test
    fun `GIVEN a lower case code WHEN resolved THEN the case does not matter`() {
        // Arrange — the resolved country is stored lower case, the wire wants it upper
        val actual = RegionDisplayName.resolve(countryCode = "us", locale = Locale.ENGLISH)

        // Assert
        assertThat(actual).isEqualTo("United States")
    }

    @Test
    fun `GIVEN another app language WHEN resolved THEN the name follows it`() {
        // Act
        val actual = RegionDisplayName.resolve(countryCode = "GB", locale = Locale("ru"))

        // Assert
        assertThat(actual).isNotEqualTo("United Kingdom")
    }

    @Test
    fun `GIVEN an unknown code WHEN resolved THEN nothing is returned`() {
        // Arrange — a name-less region must fall back to the generic wording, not print the raw code
        val actual = RegionDisplayName.resolve(countryCode = "ZZ", locale = Locale.ENGLISH)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a blank code WHEN resolved THEN nothing is returned`() {
        // Act
        val actual = RegionDisplayName.resolve(countryCode = "", locale = Locale.ENGLISH)

        // Assert
        assertThat(actual).isNull()
    }
}