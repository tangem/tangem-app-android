package com.tangem.data.polymarket.builder

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PredictionTransferCall
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketTransferCallBuilderTest {

    @Test
    fun `GIVEN the guide's worked example WHEN building THEN the call matches it byte for byte`() {
        // Act
        val actual = PolymarketTransferCallBuilder.build(
            recipient = RECIPIENT,
            amountBaseUnits = BigInteger("12500000"),
        )

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketApprovalCall(
                target = PolymarketContracts.COLLATERAL,
                value = "0",
                data = "0xa9059cbb" +
                    "0000000000000000000000005f3b00000000000000000000000000000000c1a2" +
                    "0000000000000000000000000000000000000000000000000000000000bebc20",
            ),
        )
    }

    @Test
    fun `GIVEN a built call WHEN decoded THEN the recipient and the amount come back unchanged`() {
        // Arrange
        val amount = BigInteger("12500000")
        val call = PolymarketTransferCallBuilder.build(recipient = RECIPIENT, amountBaseUnits = amount)

        // Act
        val actual = PolymarketTransferCallBuilder.decode(call)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionTransferCall(recipient = RECIPIENT.lowercase(), amountBaseUnits = amount),
        )
    }

    @Test
    fun `GIVEN a built call WHEN inspected THEN its data is exactly 68 bytes`() {
        // Act
        val actual = PolymarketTransferCallBuilder.build(
            recipient = RECIPIENT,
            amountBaseUnits = BigInteger.ONE,
        )

        // Assert
        assertThat(actual.data.removePrefix(PolymarketContracts.HEX_PREFIX)).hasLength(136)
    }

    @Test
    fun `GIVEN a call with a dirty recipient word WHEN decoded THEN it is rejected`() {
        // Arrange
        val call = PolymarketApprovalCall(
            target = PolymarketContracts.COLLATERAL,
            value = "0",
            data = "0xa9059cbb" +
                "00000000000000000000000f5f3b00000000000000000000000000000000c1a2" +
                "0000000000000000000000000000000000000000000000000000000000bebc20",
        )

        // Act
        val actual = PolymarketTransferCallBuilder.decode(call)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a call with the wrong selector WHEN decoded THEN it is rejected`() {
        // Arrange
        val built = PolymarketTransferCallBuilder.build(RECIPIENT, BigInteger.ONE)
        val call = built.copy(data = built.data.replace("a9059cbb", "23b872dd"))

        // Act
        val actual = PolymarketTransferCallBuilder.decode(call)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a call to another contract WHEN decoded THEN it is rejected`() {
        // Arrange
        val built = PolymarketTransferCallBuilder.build(RECIPIENT, BigInteger.ONE)
        val call = built.copy(target = "0x2791bca1f2de4661ed88a30c99a7a9449aa84174")

        // Act
        val actual = PolymarketTransferCallBuilder.decode(call)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a call carrying native value WHEN decoded THEN it is rejected`() {
        // Arrange
        val built = PolymarketTransferCallBuilder.build(RECIPIENT, BigInteger.ONE)
        val call = built.copy(value = "1")

        // Act
        val actual = PolymarketTransferCallBuilder.decode(call)

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a call whose data is not 68 bytes WHEN decoded THEN it is rejected`(data: String) {
        // Arrange
        val call = PolymarketTransferCallBuilder.build(RECIPIENT, BigInteger.ONE).copy(data = data)

        // Act
        val actual = PolymarketTransferCallBuilder.decode(call)

        // Assert
        assertThat(actual).isNull()
    }

    private fun provideTestModels(): List<String> {
        val built = PolymarketTransferCallBuilder.build(RECIPIENT, BigInteger.ONE).data
        return listOf(built.dropLast(2), built + "00", "0xa9059cbb", "0x")
    }

    private companion object {
        const val RECIPIENT = "0x5f3b00000000000000000000000000000000c1a2"
    }
}