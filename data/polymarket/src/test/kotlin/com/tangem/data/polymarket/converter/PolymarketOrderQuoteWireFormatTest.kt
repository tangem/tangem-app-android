package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.polymarket.models.PolymarketOrderQuoteRequest
import com.tangem.datasource.api.polymarket.models.PolymarketOrderQuoteResponse
import org.junit.jupiter.api.Test

/**
 * Pins the wire names of the quote endpoint.
 *
 * Every other DTO in this module reads its fields under the same names it declares them, so a rename is
 * caught by the compiler. This one is the exception: the wire key is `live`, while our own naming rule makes
 * the property `isLive`, so the two are bridged by an annotation that nothing else checks — drop it and the
 * response stops decoding for every wallet while the suite stays green.
 */
internal class PolymarketOrderQuoteWireFormatTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun `GIVEN the exchange's own response WHEN decoded THEN every field is read under its wire name`() {
        // Arrange — the body of a real quote, reduced to one line
        val json = """
            {"status":"FULL","side":"BUY","shares":"40.81632","notional":"2.00",
            "expectedExecutionAmount":"41.66666","averagePrice":"0.048","worstCasePrice":"0.049",
            "fees":{"market":"0.00000","builder":"0.05000","total":"0.05000"},"total":"2.05000",
            "builderCode":"0xe800ac","minOrderSize":"5","tickSize":"0.001","live":false}
        """.trimIndent()

        // Act
        val actual = moshi.adapter(PolymarketOrderQuoteResponse::class.java).fromJson(json)!!

        // Assert
        assertThat(actual.status).isEqualTo("FULL")
        assertThat(actual.side).isEqualTo("BUY")
        assertThat(actual.shares).isEqualTo("40.81632")
        assertThat(actual.notional).isEqualTo("2.00")
        assertThat(actual.expectedExecutionAmount).isEqualTo("41.66666")
        assertThat(actual.averagePrice).isEqualTo("0.048")
        assertThat(actual.worstCasePrice).isEqualTo("0.049")
        assertThat(actual.fees.total).isEqualTo("0.05000")
        assertThat(actual.total).isEqualTo("2.05000")
        assertThat(actual.builderCode).isEqualTo("0xe800ac")
        assertThat(actual.minOrderSize).isEqualTo("5")
        assertThat(actual.tickSize).isEqualTo("0.001")
        assertThat(actual.isLive).isFalse()
    }

    @Test
    fun `GIVEN a response without the live flag WHEN decoded THEN it fails instead of defaulting`() {
        // Arrange — the contract makes every field required, so a missing one is a broken contract, not a default
        val json = """
            {"status":"FULL","side":"BUY","shares":"1","notional":"1","expectedExecutionAmount":"1",
            "averagePrice":"0.1","worstCasePrice":"0.1","fees":{"market":"0","builder":"0","total":"0"},
            "total":"1","builderCode":"0x","minOrderSize":"5","tickSize":"0.001"}
        """.trimIndent()

        // Act
        val error = runCatching { moshi.adapter(PolymarketOrderQuoteResponse::class.java).fromJson(json) }
            .exceptionOrNull()

        // Assert
        assertThat(error).isNotNull()
    }

    @Test
    fun `GIVEN a request without slippage WHEN encoded THEN the key is absent rather than null`() {
        // Arrange — an absent key is what makes the backend apply its own default
        val request = PolymarketOrderQuoteRequest(
            marketId = "2944989",
            assetId = "1116047",
            side = "BUY",
            amount = "2",
            slippage = null,
        )

        // Act
        val actual = moshi.adapter(PolymarketOrderQuoteRequest::class.java).toJson(request)

        // Assert
        assertThat(actual).doesNotContain("slippage")
        assertThat(actual).contains(""""marketId":"2944989"""")
        assertThat(actual).contains(""""assetId":"1116047"""")
    }
}