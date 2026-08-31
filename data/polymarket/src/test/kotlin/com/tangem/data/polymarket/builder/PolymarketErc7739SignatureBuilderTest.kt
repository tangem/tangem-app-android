package com.tangem.data.polymarket.builder

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.approval.PolymarketContracts
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
            isNegRisk = false,
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
            isNegRisk = true,
            depositWalletAddress = DEPOSIT_WALLET,
        )

        // Assert
        assertThat(actual.toHexString().lowercase())
            .isEqualTo("97d4ba244f6511157a7682dbe340477437aa5db39aa5ccd87b221604164dbeca")
    }

    @Test
    fun `GIVEN an inner signature WHEN wrapped THEN the tail carries the domain, contents and description`() {
        // Act
        val actual = PolymarketErc7739SignatureBuilder.wrap(
            innerSignature = INNER_SIGNATURE,
            order = createOrder(),
            isNegRisk = false,
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
            isNegRisk = false,
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
            isNegRisk = false,
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

    private fun createOrder(): PredictionOrder = PredictionOrder(
        salt = 1_234_567_890L,
        maker = DEPOSIT_WALLET,
        signer = "0x4130efa9e628a991520be60f2e948a4f9be52fbc",
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
        const val HEX_CHARS_PER_BYTE = 2
    }
}