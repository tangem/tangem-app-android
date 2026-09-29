package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import org.junit.jupiter.api.Test

/**
 * The BFF wraps the geo catalogues as `{ "result": [ … ] }` — the array sits directly under `result`,
 * without a `countries` / `regions` object in between. The fixtures below are the live dev-BFF responses.
 */
internal class GeoResponsesParsingTest {

    private val json = NetworkJsonFactory.create()

    @Test
    fun `GIVEN countries response from the backend WHEN parse THEN countries are read from the result array`() {
        // Arrange
        val raw = """
            {
              "result": [
                { "code": "AF", "name": "Afghanistan", "flag_url": null, "suggested": false, "has_regions": false },
                { "code": "US", "name": "United States", "flag_url": "https://cdn.example/us.svg", "suggested": true, "has_regions": true }
              ],
              "error": null
            }
        """.trimIndent()

        // Act
        val actual = json.decodeFromString(GeoCountriesResponse.serializer(), raw)

        // Assert
        assertThat(actual).isEqualTo(
            GeoCountriesResponse(
                result = listOf(
                    GeoCountriesResponse.Country(
                        code = "AF",
                        name = "Afghanistan",
                        flagUrl = null,
                        isSuggested = false,
                        hasRegions = false,
                    ),
                    GeoCountriesResponse.Country(
                        code = "US",
                        name = "United States",
                        flagUrl = "https://cdn.example/us.svg",
                        isSuggested = true,
                        hasRegions = true,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN regions response from the backend WHEN parse THEN regions are read from the result array`() {
        // Arrange
        val raw = """
            {
              "result": [
                { "code": "AL", "name": "Alabama", "suggested": false },
                { "code": "CA", "name": "California", "suggested": true }
              ],
              "error": null
            }
        """.trimIndent()

        // Act
        val actual = json.decodeFromString(GeoRegionsResponse.serializer(), raw)

        // Assert
        assertThat(actual).isEqualTo(
            GeoRegionsResponse(
                result = listOf(
                    GeoRegionsResponse.Region(code = "AL", name = "Alabama", isSuggested = false),
                    GeoRegionsResponse.Region(code = "CA", name = "California", isSuggested = true),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN empty regions response WHEN parse THEN the result is an empty list`() {
        // Arrange
        val raw = """{ "result": [], "error": null }"""

        // Act
        val actual = json.decodeFromString(GeoRegionsResponse.serializer(), raw)

        // Assert
        assertThat(actual).isEqualTo(GeoRegionsResponse(result = emptyList()))
    }
}