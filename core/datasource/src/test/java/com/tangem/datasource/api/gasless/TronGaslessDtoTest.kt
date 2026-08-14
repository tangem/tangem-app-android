package com.tangem.datasource.api.gasless

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.datasource.api.gasless.models.GaslessServiceResponse
import com.tangem.datasource.api.gasless.models.tron.TronEstimateResponse
import com.tangem.datasource.api.gasless.models.tron.TronTokensResponse
import org.junit.jupiter.api.Test

internal class TronGaslessDtoTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Test
    fun `GIVEN estimate json WHEN parsed THEN maps all quote fields`() {
        // Arrange
        val type = com.squareup.moshi.Types.newParameterizedType(
            GaslessServiceResponse::class.java, TronEstimateResponse::class.java,
        )
        val adapter = moshi.adapter<GaslessServiceResponse<TronEstimateResponse>>(type)
        val json = """
            {"result":{"quoteId":"q_1","feeRecipient":"TFee","compensationToken":"TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
            "compensationAmount":"2.75","compensationAmountRaw":"2750000",
            "estimate":{"energy":78000,"bandwidth":345,"trxCost":"27500000"},"expiresAt":"2026-06-03T12:34:56.000Z"},
            "success":true,"timestamp":"2026-06-03T12:34:56.000Z"}
        """.trimIndent()

        // Act
        val parsed = adapter.fromJson(json)!!.result

        // Assert
        assertThat(parsed.quoteId).isEqualTo("q_1")
        assertThat(parsed.feeRecipient).isEqualTo("TFee")
        assertThat(parsed.compensationAmountRaw).isEqualTo("2750000")
        assertThat(parsed.estimate.energy).isEqualTo(78000L)
        assertThat(parsed.estimate.trxCost).isEqualTo("27500000")
        assertThat(parsed.expiresAt).isEqualTo("2026-06-03T12:34:56.000Z")
    }

    @Test
    fun `GIVEN tokens json WHEN parsed THEN maps token list`() {
        // Arrange
        val type = com.squareup.moshi.Types.newParameterizedType(
            GaslessServiceResponse::class.java, TronTokensResponse::class.java,
        )
        val adapter = moshi.adapter<GaslessServiceResponse<TronTokensResponse>>(type)
        val json = """
            {"result":{"tokens":[{"address":"TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t","symbol":"USDT","decimals":6,"chain":"Tron"}]},
            "success":true,"timestamp":"t"}
        """.trimIndent()

        // Act
        val tokens = adapter.fromJson(json)!!.result.tokens

        // Assert
        assertThat(tokens).hasSize(1)
        assertThat(tokens[0].symbol).isEqualTo("USDT")
        assertThat(tokens[0].decimals).isEqualTo(6)
    }
}