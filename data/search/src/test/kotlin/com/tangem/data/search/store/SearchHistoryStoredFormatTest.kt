package com.tangem.data.search.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.data.search.model.RecentTokenDTO
import com.tangem.data.search.model.SearchHistoryDTO
import com.tangem.data.search.model.TextHintDTO
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Guards the stored format of the search history: files written by the Moshi serializer must read back unchanged
 * after the store moved to kotlinx, and new writes keep that format.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SearchHistoryStoredFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN history WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<SearchHistoryDTO>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN history is kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson(json = model.json, defaultValue = SearchHistoryDTO())

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN history WHEN written to DataStore THEN keeps the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson(value = model.value, defaultValue = SearchHistoryDTO())

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            value = SearchHistoryDTO(textHints = listOf(HINT), recentTokens = listOf(TOKEN)),
            json = """{"textHints":[$HINT_JSON],"recentTokens":[{$TOKEN_HEAD,"imageUrl":"https://img",""" +
                """$TOKEN_MIDDLE,"marketCap":"1B","ratingPosition":"1","isUnder100kMarketCap":false,""" +
                """"stakingRateText":"4%"}]}""",
        ),
        FormatModel(
            value = SearchHistoryDTO(
                textHints = listOf(HINT),
                recentTokens = listOf(
                    TOKEN.copy(imageUrl = null, marketCap = null, ratingPosition = null, stakingRateText = null),
                ),
            ),
            json = """{"textHints":[$HINT_JSON],"recentTokens":[{$TOKEN_HEAD,$TOKEN_MIDDLE,""" +
                """"isUnder100kMarketCap":false}]}""",
        ),
    )

    data class FormatModel(val value: SearchHistoryDTO, val json: String)

    private companion object {
        val HINT = TextHintDTO(text = "btc", timestamp = 1_700_000_000_000L)
        val TOKEN = RecentTokenDTO(
            id = "bitcoin",
            name = "Bitcoin",
            symbol = "BTC",
            imageUrl = "https://img",
            timestamp = 1_700_000_000_001L,
            pricePlain = "100",
            appCurrencyCode = "USD",
            appCurrencySymbol = "$",
            priceChangePercent = "1.5%",
            priceChangeDirection = "UP",
            marketCap = "1B",
            ratingPosition = "1",
            isUnder100kMarketCap = false,
            stakingRateText = "4%",
        )
        const val HINT_JSON = """{"text":"btc","timestamp":1700000000000}"""
        const val TOKEN_HEAD = """"id":"bitcoin","name":"Bitcoin","symbol":"BTC""""
        const val TOKEN_MIDDLE = """"timestamp":1700000000001,"pricePlain":"100","appCurrencyCode":"USD",""" +
            """"appCurrencySymbol":"$","priceChangePercent":"1.5%","priceChangeDirection":"UP""""
    }
}