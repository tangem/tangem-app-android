package com.tangem.data.polymarket.builder

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketExchangeDomain
import com.tangem.domain.polymarket.model.PredictionOrder
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.utils.extensions.toHexString
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class PolymarketErc7739SignatureBuilderTest {

    @Test
    fun `GIVEN an order WHEN the nested digest is built THEN it matches the independent vector`() {
        // Act
        val actual = PolymarketErc7739SignatureBuilder.digest(
            order = createOrder(),
            domain = PolymarketExchangeDomain.CTF,
            depositWalletAddress = DEPOSIT_WALLET,
        )

        // Assert
        assertThat(actual.toHexString().lowercase())
            .isEqualTo("c36551f13eb9491a93253a7125e9d72b9dee9b7a1bc1c4d2f0c33c684d566324")
    }

    @Test
    fun `GIVEN a neg-risk market WHEN the nested digest is built THEN only the outer domain changes it`() {
        // Act
        val actual = PolymarketErc7739SignatureBuilder.digest(
            order = createOrder(),
            domain = PolymarketExchangeDomain.NEG_RISK_CTF,
            depositWalletAddress = DEPOSIT_WALLET,
        )

        // Assert
        assertThat(actual.toHexString().lowercase())
            .isEqualTo("97d4ba244f6511157a7682dbe340477437aa5db39aa5ccd87b221604164dbeca")
    }

    @Test
    fun `GIVEN a deposit wallet order on the V3 exchange WHEN the nested digest is built THEN it matches the SDK`() {
        // Act
        val actual = PolymarketErc7739SignatureBuilder.digest(
            order = createOrder(signer = DEPOSIT_WALLET),
            domain = PolymarketExchangeDomain.EXCHANGE_V3,
            depositWalletAddress = DEPOSIT_WALLET,
        )

        // Assert
        assertThat(actual.toHexString().lowercase())
            .isEqualTo("f4e572f732eef81f7cf95b5ffa3648205e9c9a799bd205be14c8ec95b6acb914")
    }

    @Test
    fun `GIVEN the SDK inner signature for the V3 exchange WHEN wrapped THEN it matches the SDK signature`() {
        // Act
        val actual = PolymarketErc7739SignatureBuilder.wrap(
            innerSignature = SDK_V3_INNER_SIGNATURE,
            order = createOrder(signer = DEPOSIT_WALLET),
            domain = PolymarketExchangeDomain.EXCHANGE_V3,
        )

        // Assert
        assertThat(actual).isEqualTo(SDK_V3_SIGNATURE)
    }

    @Test
    fun `GIVEN an inner signature WHEN wrapped THEN the tail carries the domain, contents and description`() {
        // Act
        val actual = PolymarketErc7739SignatureBuilder.wrap(
            innerSignature = INNER_SIGNATURE,
            order = createOrder(),
            domain = PolymarketExchangeDomain.CTF,
        )

        // Assert
        assertThat(actual).startsWith(INNER_SIGNATURE)
        assertThat(actual).contains("3264e159346253e26a64e00b69032db0e7d32f94628de3e6eecb50304d7af3d2")
        assertThat(actual).contains("345bc1910f4a991a6b3deb33f798f6fbb2536a185d922c896dd8daee671bcd05")
        assertThat(actual).contains(PolymarketOrderTypedDataBuilder.orderTypeString().toByteArray().toHexString().lowercase())
        assertThat(actual).endsWith("00ba")
    }

    @Test
    fun `GIVEN a wrapped signature WHEN measured THEN the tail after v is the expected size`() {
        // Act
        val actual = PolymarketErc7739SignatureBuilder.wrap(
            innerSignature = INNER_SIGNATURE,
            order = createOrder(),
            domain = PolymarketExchangeDomain.CTF,
        )

        // Assert
        val tail = actual.removePrefix("0x").length - INNER_SIGNATURE.removePrefix("0x").length
        val tailBytes = tail / HEX_CHARS_PER_BYTE
        assertThat(tailBytes).isEqualTo(PolymarketErc7739SignatureBuilder.TAIL_SIZE_AFTER_V)
    }

    @Test
    fun `GIVEN a wrapped signature WHEN read THEN it is prefixed hex like every other signature we send`() {
        // Act
        val actual = PolymarketErc7739SignatureBuilder.wrap(
            innerSignature = INNER_SIGNATURE,
            order = createOrder(),
            domain = PolymarketExchangeDomain.CTF,
        )

        // Assert
        assertThat(actual).startsWith("0x")
    }

    @Test
    fun `GIVEN the description WHEN read THEN it ends with a bracket so the wallet derives the type name`() {
        // Assert
        assertThat(PolymarketOrderTypedDataBuilder.orderTypeString()).endsWith(")")
        assertThat(PolymarketOrderTypedDataBuilder.orderTypeString()).startsWith("Order(")
    }

    private fun createOrder(
        signer: String = "0x4130efa9e628a991520be60f2e948a4f9be52fbc",
    ): PredictionOrder = PredictionOrder(
        salt = 1_234_567_890L,
        maker = DEPOSIT_WALLET,
        signer = signer,
        tokenId = BigInteger("1116017998421700000000000000000000000000000000000000000000000000047"),
        makerAmount = BigInteger("72000000"),
        takerAmount = BigInteger("97959180"),
        side = PredictionOrderSide.BUY,
        signatureType = PolymarketContracts.SIGNATURE_TYPE_DEPOSIT_WALLET,
        timestampMillis = 1_787_300_000_000L,
        metadata = "0x0000000000000000000000000000000000000000000000000000000000000000",
        builder = "0x00000000000000000000000000000000000000000000000000000000000000aa",
    )

    private companion object {
        const val DEPOSIT_WALLET = "0x5A057dc6394196fBa51D84457DA3FB8FA241Fc20"
        val INNER_SIGNATURE = "0x" + "11".repeat(n = 65)
        val SDK_V3_INNER_SIGNATURE =
            "0xe1ee2721d16b7c0505142c9b8e98d4e624a4667b8b9cb067468faa4a71c3b2302889c996e6e0025bf3210a13695fd7" +
            "5d0d6c34fd129c87acff605e807072a6711c"
        val SDK_V3_SIGNATURE =
            "0xe1ee2721d16b7c0505142c9b8e98d4e624a4667b8b9cb067468faa4a71c3b2302889c996e6e0025bf3210a13695fd7" +
            "5d0d6c34fd129c87acff605e807072a6711c466c63910185bbd55e8679264200c4e0abdcbb0c6264eb3d41d13326022e" +
            "095b0de04ac770d2773a40593ec74a9899ef1beca6359c6152ee7a570afd328e3d994f726465722875696e7432353620" +
            "73616c742c61646472657373206d616b65722c61646472657373207369676e65722c75696e7432353620746f6b656e49" +
            "642c75696e74323536206d616b6572416d6f756e742c75696e743235362074616b6572416d6f756e742c75696e743820" +
            "736964652c75696e7438207369676e6174757265547970652c75696e743235362074696d657374616d702c6279746573" +
            "3332206d657461646174612c62797465733332206275696c6465722900ba"
        const val HEX_CHARS_PER_BYTE = 2
    }
}