package com.tangem.domain.jointaccount.safe

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class Erc20TransferEncoderTest {

    @Test
    fun `GIVEN a recipient and an amount WHEN encoded THEN the calldata is the canonical transfer call`() {
        // Act
        val actual = Erc20TransferEncoder.encodeTransfer(
            recipient = "0x1111111111111111111111111111111111111111",
            amount = BigInteger("2500000"),
        )

        // Assert
        assertThat(actual).isEqualTo(
            "0xa9059cbb" +
                "0000000000000000000000001111111111111111111111111111111111111111" +
                "00000000000000000000000000000000000000000000000000000000002625a0",
        )
    }

    @Test
    fun `GIVEN a checksummed recipient WHEN encoded THEN the calldata is lowercase`() {
        // Act
        val actual = Erc20TransferEncoder.encodeTransfer(
            recipient = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
            amount = BigInteger.ONE,
        )

        // Assert
        assertThat(actual).isEqualTo(
            "0xa9059cbb" +
                "000000000000000000000000a0b86991c6218b36c1d19d4a2e9eb0ce3606eb48" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        )
    }

    @Test
    fun `GIVEN an amount beyond uint256 WHEN encoded THEN encoding fails instead of truncating`() {
        // Arrange
        val beyondUint256 = BigInteger.ONE.shiftLeft(256)

        // Act
        val maxFits = runCatching {
            Erc20TransferEncoder.encodeTransfer(recipient = "0x1111111111111111111111111111111111111111", amount = beyondUint256 - BigInteger.ONE)
        }
        val actual = runCatching {
            Erc20TransferEncoder.encodeTransfer(recipient = "0x1111111111111111111111111111111111111111", amount = beyondUint256)
        }.exceptionOrNull()

        // Assert
        assertThat(maxFits.isSuccess).isTrue()
        assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN a recipient with an odd hex length WHEN encoded THEN encoding fails instead of dropping a nibble`() {
        // Arrange
        val oddLengthRecipient = "0x" + "1".repeat(41)

        // Act
        val actual = runCatching {
            Erc20TransferEncoder.encodeTransfer(recipient = oddLengthRecipient, amount = BigInteger.ONE)
        }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN a malformed recipient WHEN encoded THEN encoding fails instead of shifting the word`() {
        // Act
        val actual = runCatching {
            Erc20TransferEncoder.encodeTransfer(recipient = "0x1111", amount = BigInteger.ONE)
        }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
    }
}