package com.tangem.data.polymarket.builder

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionOrder
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class PolymarketPlaceOrderBodyBuilderTest {

    @Test
    fun `GIVEN a signed buy order WHEN serialised THEN the body is byte for byte the expected json`() {
        // Act
        val actual = PolymarketPlaceOrderBodyBuilder.build(signedOrder = createSignedOrder(), apiKey = API_KEY)

        // Assert
        assertThat(actual).isEqualTo(
            """{"order":{"salt":"1234567890","maker":"0xMaker","signer":"0xSigner","tokenId":"7","""" +
                """makerAmount":"72000000","takerAmount":"97959180","side":"BUY","signatureType":3,""" +
                """"timestamp":"1787300000000","metadata":"0xmeta","builder":"0xbuilder",""" +
                """"signature":"0xsig"},"owner":"$API_KEY","orderType":"FAK"}""",
        )
    }

    @Test
    fun `GIVEN a sell order WHEN serialised THEN the side is the name and not the struct number`() {
        // Act
        val actual = PolymarketPlaceOrderBodyBuilder.build(
            signedOrder = createSignedOrder(side = PredictionOrderSide.SELL),
            apiKey = API_KEY,
        )

        // Assert
        assertThat(actual).contains(""""side":"SELL"""")
    }

    @Test
    fun `GIVEN any order WHEN serialised THEN it is fill-and-kill`() {
        // Act
        val actual = PolymarketPlaceOrderBodyBuilder.build(signedOrder = createSignedOrder(), apiKey = API_KEY)

        // Assert
        assertThat(actual).contains(""""orderType":"FAK"""")
    }

    private fun createSignedOrder(side: PredictionOrderSide = PredictionOrderSide.BUY): PredictionSignedOrder =
        PredictionSignedOrder(
            order = PredictionOrder(
                salt = BigInteger("1234567890"),
                maker = "0xMaker",
                signer = "0xSigner",
                tokenId = BigInteger("7"),
                makerAmount = BigInteger("72000000"),
                takerAmount = BigInteger("97959180"),
                side = side,
                signatureType = 3,
                timestampMillis = 1_787_300_000_000L,
                metadata = "0xmeta",
                builder = "0xbuilder",
            ),
            signature = "0xsig",
        )

    private companion object {
        const val API_KEY = "6b2e0b4e-0000-4000-8000-000000000000"
    }
}