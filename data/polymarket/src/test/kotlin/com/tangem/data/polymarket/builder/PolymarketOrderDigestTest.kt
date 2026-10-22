package com.tangem.data.polymarket.builder

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.domain.polymarket.model.PolymarketExchangeDomain
import com.tangem.domain.polymarket.model.PredictionOrder
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.extensions.toHexString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.web3j.crypto.StructuredDataEncoder
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
            PolymarketOrderTypedDataBuilder.domainSeparator(PolymarketExchangeDomain.CTF)
                .toHexString()
                .lowercase()
                .let { "0x$it" },
        ).isEqualTo("0x3264e159346253e26a64e00b69032db0e7d32f94628de3e6eecb50304d7af3d2")
    }

    @Test
    fun `GIVEN the neg-risk exchange WHEN its domain is hashed THEN it matches the value in its bytecode`() {
        // Assert
        assertThat(
            PolymarketOrderTypedDataBuilder.domainSeparator(PolymarketExchangeDomain.NEG_RISK_CTF)
                .toHexString()
                .lowercase()
                .let { "0x$it" },
        ).isEqualTo("0x9b858f53327b0bd13af8ec14cfb35234fb9eb7b0504d1a4e61f433840d30e81a")
    }

    @Test
    fun `GIVEN the V3 exchange WHEN its domain is hashed THEN it matches the separator the contract reports`() {
        // Assert
        assertThat(
            PolymarketOrderTypedDataBuilder.domainSeparator(PolymarketExchangeDomain.EXCHANGE_V3)
                .toHexString()
                .lowercase()
                .let { "0x$it" },
        ).isEqualTo("0x466c63910185bbd55e8679264200c4e0abdcbb0c6264eb3d41d13326022e095b")
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a deposit wallet order WHEN hashed for an exchange THEN it matches the Polymarket SDK digest`(
        model: SdkDigestModel,
    ) {
        // Act
        val actual = PolymarketOrderTypedDataBuilder.digest(
            order = createOrder(side = model.side, signer = DEPOSIT_WALLET),
            domain = model.domain,
        )

        // Assert
        assertThat(actual.toHexString().lowercase().let { "0x$it" }).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<SdkDigestModel> = listOf(
        SdkDigestModel(
            domain = PolymarketExchangeDomain.CTF,
            side = PredictionOrderSide.BUY,
            expected = "0xa7fea8aa10e918a92ce2cc90c2c02715659dd5e75514be976d33b86fb434955a",
        ),
        SdkDigestModel(
            domain = PolymarketExchangeDomain.CTF,
            side = PredictionOrderSide.SELL,
            expected = "0x0046f3b3af63ebbe2777a48f8db1bbecaaffc3479b789f5f0b5eca20c4d6d9f7",
        ),
        SdkDigestModel(
            domain = PolymarketExchangeDomain.NEG_RISK_CTF,
            side = PredictionOrderSide.BUY,
            expected = "0xdf34a057ab55336f5a878e581f0c480e553de1a2e612f0959dd8057b741eb1f2",
        ),
        SdkDigestModel(
            domain = PolymarketExchangeDomain.NEG_RISK_CTF,
            side = PredictionOrderSide.SELL,
            expected = "0xc051210f320712524247fc2897b0d0d8e71dd5c10cbced14b3f03f4333024b53",
        ),
        SdkDigestModel(
            domain = PolymarketExchangeDomain.EXCHANGE_V3,
            side = PredictionOrderSide.BUY,
            expected = "0xbead8b62f4b223d14ea34655ef692aa65efc7f0a665dd0147088c1821b97ed70",
        ),
        SdkDigestModel(
            domain = PolymarketExchangeDomain.EXCHANGE_V3,
            side = PredictionOrderSide.SELL,
            expected = "0xc3e6f4e67ca95d0304db4a71ef81e92083c2166e9821ee1004152133e27423af",
        ),
    )

    @Test
    /** The expected digest comes from a separate implementation (Python, pycryptodome), not from this code. */
    fun `GIVEN an order on a standard market WHEN hashed THEN it matches the independent digest`() {
        // Act
        val actual = PolymarketOrderTypedDataBuilder.digest(
            order = createOrder(),
            domain = PolymarketExchangeDomain.CTF,
        )

        // Assert
        assertThat(actual.toHexString().lowercase().let { "0x$it" })
            .isEqualTo("0xb47422d962a0c68eb293d767622ba49f81ef26ae12ed4c42881b8cb0c068e098")
    }

    @Test
    fun `GIVEN the same order on a neg-risk market WHEN hashed THEN only the domain changes the digest`() {
        // Act
        val actual = PolymarketOrderTypedDataBuilder.digest(
            order = createOrder(),
            domain = PolymarketExchangeDomain.NEG_RISK_CTF,
        )

        // Assert
        assertThat(actual.toHexString().lowercase().let { "0x$it" })
            .isEqualTo("0xd6e520c7b039361908546367d4fc4c60cd7d7e6fec2c0f69912d0ecf6289a1d8")
    }

    @Test
    fun `GIVEN an order WHEN hashed by hand THEN two independent typed-data encoders agree`() {
        // Arrange
        val json = PolymarketOrderTypedDataBuilder.buildOrderJson(
            order = createOrder(),
            domain = PolymarketExchangeDomain.CTF,
        )

        // Act
        val manual = PolymarketOrderTypedDataBuilder.digest(
            order = createOrder(),
            domain = PolymarketExchangeDomain.CTF,
        )

        // Assert
        assertThat(EthereumUtils.makeTypedDataHash(json).toHexString()).isEqualTo(manual.toHexString())
        assertThat(StructuredDataEncoder(json).hashStructuredData().toHexString()).isEqualTo(manual.toHexString())
    }

    @Test
    fun `GIVEN a sell order WHEN hashed THEN the side changes the digest`() {
        // Act
        val buy = PolymarketOrderTypedDataBuilder.digest(order = createOrder(), domain = PolymarketExchangeDomain.CTF)
        val sell = PolymarketOrderTypedDataBuilder.digest(
            order = createOrder(side = PredictionOrderSide.SELL),
            domain = PolymarketExchangeDomain.CTF,
        )

        // Assert
        assertThat(sell.toHexString()).isNotEqualTo(buy.toHexString())
    }

    private fun createOrder(
        side: PredictionOrderSide = PredictionOrderSide.BUY,
        signer: String = "0x4130efa9e628a991520be60f2e948a4f9be52fbc",
    ): PredictionOrder = PredictionOrder(
        salt = 1_234_567_890L,
        maker = DEPOSIT_WALLET,
        signer = signer,
        tokenId = BigInteger("1116017998421700000000000000000000000000000000000000000000000000047"),
        makerAmount = BigInteger("72000000"),
        takerAmount = BigInteger("97959180"),
        side = side,
        signatureType = 3,
        timestampMillis = 1_787_300_000_000L,
        metadata = "0x0000000000000000000000000000000000000000000000000000000000000000",
        builder = "0x00000000000000000000000000000000000000000000000000000000000000aa",
    )

    internal data class SdkDigestModel(
        val domain: PolymarketExchangeDomain,
        val side: PredictionOrderSide,
        val expected: String,
    )

    private companion object {
        const val DEPOSIT_WALLET = "0x5A057dc6394196fBa51D84457DA3FB8FA241Fc20"
    }
}