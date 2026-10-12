package com.tangem.data.quotes.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Guards the stored format of the quotes: files written by the Moshi serializer must read back unchanged after the
 * store moved to kotlinx, and new writes keep that format, so the Moshi serializer that is still used while the
 * kotlinx DataStore toggle is off reads them too.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class QuotesStoredFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN quotes WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Map<String, QuotesResponse.Quote>>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN decoded by Moshi THEN quotes are kept`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Map<String, QuotesResponse.Quote>>().fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN quotes are kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson(json = model.json, defaultValue = emptyMap<String, QuotesResponse.Quote>())

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN quotes WHEN written to DataStore THEN keeps the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson(value = model.value, defaultValue = emptyMap())

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            value = mapOf(
                "bitcoin" to QuotesResponse.Quote(
                    price = BigDecimal("65000.5"),
                    priceChange24h = BigDecimal("-1.25"),
                    priceChange1w = BigDecimal("0.00000001"),
                    priceChange30d = BigDecimal("10"),
                    priceUsd = BigDecimal("65000.5"),
                ),
            ),
            json = """{"bitcoin":{"price":"65000.5","priceChange24h":"-1.25","priceChange1w":"1E-8",""" +
                """"priceChange30d":"10","priceUsd":"65000.5"}}""",
        ),
        FormatModel(value = mapOf("unknown" to QuotesResponse.Quote.EMPTY), json = """{"unknown":{}}"""),
    )

    data class FormatModel(val value: Map<String, QuotesResponse.Quote>, val json: String)
}