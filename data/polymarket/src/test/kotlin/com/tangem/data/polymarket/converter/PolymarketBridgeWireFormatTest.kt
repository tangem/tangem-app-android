package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketBridgeAddressesDto
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
            {"supportedAssets":[{"chainId":137,"chainName":"Polygon",
            "token":{"name":"USD Coin","symbol":"USDC",
            "address":"0x3c499c542cef5e3811e1192ce70d8cc03d5c3359","decimals":6},
            "minCheckoutUsd":2.0}]}
        """.trimIndent()

        // Act
        val actual = moshi.adapter(PolymarketSupportedAssetsResponse::class.java).fromJson(json)!!

        // Assert
        val asset = actual.supportedAssets.single()
        assertThat(asset.chainId).isEqualTo(137L)
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
            {"supportedAssets":[{"chainId":137,"chainName":"Polygon",
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
    fun `GIVEN an address payload naming Tron tvm WHEN decoded THEN evm survives and nothing throws`() {
        // Arrange
        val json = """{"evm":"0xabc","svm":"sol1","btc":"bc1","tvm":"T9y"}"""

        // Act
        val actual = moshi.adapter(PolymarketBridgeAddressesDto::class.java).fromJson(json)!!

        // Assert
        assertThat(actual.evm).isEqualTo("0xabc")
        assertThat(actual.svm).isEqualTo("sol1")
        assertThat(actual.btc).isEqualTo("bc1")
        assertThat(actual.tron).isNull()
    }

    @Test
    fun `GIVEN an address payload carrying only evm WHEN decoded THEN the other families are absent`() {
        // Arrange
        val json = """{"evm":"0xabc"}"""

        // Act
        val actual = moshi.adapter(PolymarketBridgeAddressesDto::class.java).fromJson(json)!!

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