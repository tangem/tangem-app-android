package com.tangem.features.send.send.joint

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class JointTransferIntentFactoryTest {

    private val token = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
    )

    @Test
    fun `GIVEN a coin amount WHEN created THEN a native intent carries the amount in wei`() {
        // Arrange
        val amountUM = createAmountData(crypto = BigDecimal("1.5"))

        // Act
        val actual = JointTransferIntentFactory.create(amountUM, createDestinationContent(), fixtureCoin)

        // Assert
        assertThat(actual).isEqualTo(
            JointTransferIntent.Native(recipient = FIXTURE_RECIPIENT, amountWei = BigInteger("1500000000000000000")),
        )
    }

    @Test
    fun `GIVEN a token amount WHEN created THEN an erc20 intent targets the contract`() {
        // Arrange
        val amountUM = createAmountData(crypto = BigDecimal("2"))

        // Act
        val actual = JointTransferIntentFactory.create(amountUM, createDestinationContent(), token)

        // Assert
        assertThat(actual).isEqualTo(
            JointTransferIntent.Erc20(
                tokenContract = token.contractAddress,
                recipient = FIXTURE_RECIPIENT,
                amountWei = BigDecimal("2").movePointRight(token.decimals).toBigInteger(),
            ),
        )
    }

    @Test
    fun `GIVEN an empty amount WHEN created THEN there is no intent`() {
        // Act
        val actual = JointTransferIntentFactory.create(AmountState.Empty, createDestinationContent(), fixtureCoin)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a zero amount WHEN created THEN there is no intent`() {
        // Act
        val actual = JointTransferIntentFactory.create(
            createAmountData(crypto = BigDecimal.ZERO),
            createDestinationContent(),
            fixtureCoin,
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN no destination WHEN created THEN there is no intent`() {
        // Act
        val actual = JointTransferIntentFactory.create(
            createAmountData(crypto = BigDecimal.ONE),
            DestinationUM.Empty(),
            fixtureCoin,
        )

        // Assert
        assertThat(actual).isNull()
    }
}