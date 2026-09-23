package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketDepositResponse
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketDepositRequest
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketSupportedAssetsResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PolymarketBridgeWireFormatTest {

    private class BigDecimalAdapter {
        @FromJson
        fun fromJson(value: String) = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal) = value.toString()
    }

    private val moshi = Moshi.Builder().add(BigDecimalAdapter()).build()

    @Test
    fun `GIVEN the bridge's asset list WHEN decoded THEN the minimum is read as a number`() {
        // Arrange
        val json = """
            {"supportedAssets":[{"chainId":"137","chainName":"Polygon",
            "token":{"name":"USD Coin","symbol":"USDC",
            "address":"0x3c499c542cef5e3811e1192ce70d8cc03d5c3359","decimals":6},
            "minCheckoutUsd":2.0}]}
        """.trimIndent()

        // Act
        val actual = moshi.adapter(PolymarketSupportedAssetsResponse::class.java).fromJson(json)!!

        // Assert
        val asset = actual.supportedAssets.single()
        assertThat(asset.chainId).isEqualTo("137")
        assertThat(asset.chainName).isEqualTo("Polygon")
        assertThat(asset.token.symbol).isEqualTo("USDC")
        assertThat(asset.token.address).isEqualTo("0x3c499c542cef5e3811e1192ce70d8cc03d5c3359")
        assertThat(asset.token.decimals).isEqualTo(6)
        assertThat(asset.minCheckoutUsd).isEqualTo(BigDecimal("2.0"))
    }

    @Test
    fun `GIVEN the minimum arrives as a string WHEN decoded THEN it reads the same as a number`() {
        // Arrange
        val asNumber = """
            {"supportedAssets":[{"chainId":"137","chainName":"Polygon",
            "token":{"name":"USD Coin","symbol":"USDC","address":"0x3c49","decimals":6},
            "minCheckoutUsd":2.50}]}
        """.trimIndent()
        val asString = asNumber.replace(""""minCheckoutUsd":2.50""", """"minCheckoutUsd":"2.50"""")
        val adapter = moshi.adapter(PolymarketSupportedAssetsResponse::class.java)

        // Act
        val fromNumber = adapter.fromJson(asNumber)!!.supportedAssets.single().minCheckoutUsd
        val fromString = adapter.fromJson(asString)!!.supportedAssets.single().minCheckoutUsd

        // Assert
        assertThat(fromNumber).isEqualTo(BigDecimal("2.50"))
        assertThat(fromString).isEqualTo(BigDecimal("2.50"))
    }

    @Test
    fun `GIVEN the deposit payload the bridge answers with WHEN decoded THEN every family is read`() {
        // Arrange — captured from bridge.polymarket.com on 03.09.2026
        val json = """
            {
                "address": {
                    "evm": "0x4038A7a1Bf34cbcB02528A23184d18f4AD631D2a",
                    "svm": "8MhRonoqhv1X3P8jG17EJwvbkY9WWpR5qbxanRwWxWdt",
                    "tron": "TMSM4Sy4V6faXVvry5Y9mHoEZf7oSjnM1Y",
                    "btc": "bc1q9wshqzjuk3y8mf2mmnmk0vwutuy0y2ru7jtvyp"
                },
                "note": "Only certain chains and tokens are supported."
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(PolymarketDepositResponse::class.java).fromJson(json)!!.address

        // Assert
        assertThat(actual.evm).isEqualTo("0x4038A7a1Bf34cbcB02528A23184d18f4AD631D2a")
        assertThat(actual.svm).isEqualTo("8MhRonoqhv1X3P8jG17EJwvbkY9WWpR5qbxanRwWxWdt")
        assertThat(actual.tron).isEqualTo("TMSM4Sy4V6faXVvry5Y9mHoEZf7oSjnM1Y")
        assertThat(actual.btc).isEqualTo("bc1q9wshqzjuk3y8mf2mmnmk0vwutuy0y2ru7jtvyp")
    }

    @Test
    fun `GIVEN a deposit payload carrying only evm WHEN decoded THEN the other families are absent`() {
        // Arrange
        val json = """{"address":{"evm":"0xabc"}}"""

        // Act
        val actual = moshi.adapter(PolymarketDepositResponse::class.java).fromJson(json)!!.address

        // Assert
        assertThat(actual.evm).isEqualTo("0xabc")
        assertThat(actual.svm).isNull()
        assertThat(actual.btc).isNull()
        assertThat(actual.tron).isNull()
    }

    @Test
    fun `GIVEN a deposit request WHEN encoded THEN the deposit wallet is sent under address`() {
        // Arrange
        val request = PolymarketDepositRequest(address = "0xdef0")

        // Act
        val actual = moshi.adapter(PolymarketDepositRequest::class.java).toJson(request)

        // Assert
        assertThat(actual).isEqualTo("""{"address":"0xdef0"}""")
    }
}