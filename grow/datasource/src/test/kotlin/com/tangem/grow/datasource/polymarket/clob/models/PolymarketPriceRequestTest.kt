package com.tangem.grow.datasource.polymarket.clob.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.jupiter.api.Test

internal class PolymarketPriceRequestTest {

    private val moshi = Moshi.Builder().build()
    private val sidePricesType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val responseType = Types.newParameterizedType(Map::class.java, String::class.java, sidePricesType)
    private val responseAdapter = moshi.adapter<Map<String, Map<String, String>>>(responseType)

    @Test
    fun `GIVEN token and book side WHEN encoded THEN request uses CLOB field names`() {
        // Arrange
        val request = PolymarketPriceRequest(tokenId = "123", side = "SELL")

        // Act
        val json = moshi.adapter(PolymarketPriceRequest::class.java).toJson(request)

        // Assert
        assertThat(json).isEqualTo("""{"token_id":"123","side":"SELL"}""")
    }

    @Test
    fun `GIVEN numeric and string prices WHEN decoded THEN decimal text and both sides are preserved`() {
        // Arrange
        val json = """{"123":{"BUY":0.42,"SELL":"0.430"},"456":{"SELL":0.001}}"""

        // Act
        val prices = requireNotNull(responseAdapter.fromJson(json))

        // Assert
        assertThat(prices["123"]).containsExactly("BUY", "0.42", "SELL", "0.430")
        assertThat(prices["456"]).containsExactly("SELL", "0.001")
    }

}