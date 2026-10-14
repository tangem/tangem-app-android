package com.tangem.grow.datasource.polymarket.data.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketPositionsValueResponseTest {

    private class BigDecimalAdapter {
        @FromJson
        fun fromJson(value: String) = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal) = value.toString()
    }

    private val adapter = Moshi.Builder()
        .add(BigDecimalAdapter())
        .build()
        .adapter(PolymarketPositionsValueResponse::class.java)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a data API v2 body WHEN decoded THEN the wallet total is read from the envelope`(model: DecodeModel) {
        // Act
        val actual = adapter.fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        DecodeModel(
            json = """{"data":{"proxy_wallet":"0xdw","value":4.7834}}""",
            expected = createResponse(value = BigDecimal("4.7834")),
        ),
        DecodeModel(
            json = """{"data":{"proxy_wallet":"0xdw","value":0.0}}""",
            expected = createResponse(value = BigDecimal("0.0")),
        ),
    )

    private fun createResponse(value: BigDecimal) = PolymarketPositionsValueResponse(
        data = PolymarketPositionsValueResponse.Data(value = value),
    )

    internal data class DecodeModel(val json: String, val expected: PolymarketPositionsValueResponse)
}