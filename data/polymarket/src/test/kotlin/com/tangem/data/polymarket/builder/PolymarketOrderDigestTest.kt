package com.tangem.data.polymarket.builder

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.domain.polymarket.model.PredictionOrder
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.utils.extensions.toHexString
import org.junit.jupiter.api.Test
import org.web3j.crypto.StructuredDataEncoder
import java.math.BigInteger

internal class PolymarketOrderDigestTest {

    @Test
    /** The expected hash is Polymarket's own; the type string is the only candidate spelling reproducing it. */
    fun `GIVEN the order type string WHEN hashed THEN it matches the documented type hash`() {
        // Assert
        assertThat(PolymarketOrderTypedDataBuilder.orderTypeHash().toHexString().lowercase().let { "0x$it" })
            .isEqualTo("0xbb86318a2138f5fa8ae32fbe8e659f8fcf13cc6ae4014a707893055433818589")
    }

    @Test
    /**
     * The expected separator was read out of the exchange's deployed runtime bytecode on Polygon, where it
     * sits as an immutable — see `knowledge-base/17-order-signing-vectors-2026-08-21.md` to reproduce it
     * with `eth_getCode`.
     */
    fun `GIVEN the standard exchange WHEN its domain is hashed THEN it matches the value in its bytecode`() {
        // Assert
        assertThat(
            PolymarketOrderTypedDataBuilder.domainSeparator(isNegRisk = false).toHexString().lowercase().let { "0x$it" },
        ).isEqualTo("0x3264e159346253e26a64e00b69032db0e7d32f94628de3e6eecb50304d7af3d2")
    }

    @Test
    fun `GIVEN the neg-risk exchange WHEN its domain is hashed THEN it matches the value in its bytecode`() {
        // Assert
        assertThat(
            PolymarketOrderTypedDataBuilder.domainSeparator(isNegRisk = true).toHexString().lowercase().let { "0x$it" },
        ).isEqualTo("0x9b858f53327b0bd13af8ec14cfb35234fb9eb7b0504d1a4e61f433840d30e81a")
    }

    @Test
    /** The expected digest comes from a separate implementation (Python, pycryptodome), not from this code. */
    fun `GIVEN an order on a standard market WHEN hashed THEN it matches the independent digest`() {
        // Act
        val actual = PolymarketOrderTypedDataBuilder.digest(order = createOrder(), isNegRisk = false)

        // Assert
        assertThat(actual.toHexString().lowercase().let { "0x$it" })
            .isEqualTo("0xb47422d962a0c68eb293d767622ba49f81ef26ae12ed4c42881b8cb0c068e098")
    }

    @Test
    fun `GIVEN the same order on a neg-risk market WHEN hashed THEN only the domain changes the digest`() {
        // Act
        val actual = PolymarketOrderTypedDataBuilder.digest(order = createOrder(), isNegRisk = true)

        // Assert
        assertThat(actual.toHexString().lowercase().let { "0x$it" })
            .isEqualTo("0xd6e520c7b039361908546367d4fc4c60cd7d7e6fec2c0f69912d0ecf6289a1d8")
    }

    @Test
    fun `GIVEN an order WHEN hashed by hand THEN two independent typed-data encoders agree`() {
        // Arrange
        val json = PolymarketOrderTypedDataBuilder.buildOrderJson(order = createOrder(), isNegRisk = false)

        // Act
        val manual = PolymarketOrderTypedDataBuilder.digest(order = createOrder(), isNegRisk = false)

        // Assert
        assertThat(EthereumUtils.makeTypedDataHash(json).toHexString()).isEqualTo(manual.toHexString())
        assertThat(StructuredDataEncoder(json).hashStructuredData().toHexString()).isEqualTo(manual.toHexString())
    }

    @Test
    fun `GIVEN a sell order WHEN hashed THEN the side changes the digest`() {
        // Act
        val buy = PolymarketOrderTypedDataBuilder.digest(order = createOrder(), isNegRisk = false)
        val sell = PolymarketOrderTypedDataBuilder.digest(
            order = createOrder(side = PredictionOrderSide.SELL),
            isNegRisk = false,
        )

        // Assert
        assertThat(sell.toHexString()).isNotEqualTo(buy.toHexString())
    }

    private fun createOrder(side: PredictionOrderSide = PredictionOrderSide.BUY): PredictionOrder = PredictionOrder(
        salt = 1_234_567_890L,
        maker = "0x5A057dc6394196fBa51D84457DA3FB8FA241Fc20",
        signer = "0x4130efa9e628a991520be60f2e948a4f9be52fbc",
        tokenId = BigInteger("1116017998421700000000000000000000000000000000000000000000000000047"),
        makerAmount = BigInteger("72000000"),
        takerAmount = BigInteger("97959180"),
        side = side,
        signatureType = 3,
        timestampMillis = 1_787_300_000_000L,
        metadata = "0x0000000000000000000000000000000000000000000000000000000000000000",
        builder = "0x00000000000000000000000000000000000000000000000000000000000000aa",
    )
}