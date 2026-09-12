package com.tangem.data.jointaccount.safe

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeTx
import org.junit.jupiter.api.Test
import java.math.BigInteger

/**
 * The encoded calldata is pinned byte for byte: it is what the co-signer's verification compares the operation
 * against, and what the Safe finally executes. The vectors are the canonical `transfer(address,uint256)` calls.
 */
internal class DefaultJointTransferPayloadCodecTest {

    private val codec = DefaultJointTransferPayloadCodec()

    @Test
    fun `GIVEN a token transfer WHEN encoded THEN the calldata is the canonical transfer call`() {
        // Act
        val actual = codec.encode(
            JointTransferIntent.Erc20(
                tokenContract = TOKEN,
                recipient = RECIPIENT,
                amountWei = BigInteger("2500000"),
            ),
        )

        // Assert
        assertThat(actual).isEqualTo(
            SafeInnerCall(
                to = TOKEN,
                value = BigInteger.ZERO,
                data = "0xa9059cbb" +
                    "0000000000000000000000001111111111111111111111111111111111111111" +
                    "00000000000000000000000000000000000000000000000000000000002625a0",
            ),
        )
    }

    @Test
    fun `GIVEN a checksummed recipient WHEN encoded THEN the calldata is lowercase`() {
        // Act
        val actual = codec.encode(
            JointTransferIntent.Erc20(
                tokenContract = TOKEN.uppercase(),
                recipient = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
                amountWei = BigInteger.ONE,
            ),
        )

        // Assert
        assertThat(actual?.data).isEqualTo(
            "0xa9059cbb" +
                "000000000000000000000000a0b86991c6218b36c1d19d4a2e9eb0ce3606eb48" +
                "0000000000000000000000000000000000000000000000000000000000000001",
        )
        assertThat(actual?.to).isEqualTo(TOKEN)
    }

    @Test
    fun `GIVEN a coin transfer WHEN encoded THEN the call carries the value and no calldata`() {
        // Act
        val actual = codec.encode(JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT))

        // Assert
        assertThat(actual).isEqualTo(SafeInnerCall(to = RECIPIENT, value = AMOUNT, data = "0x"))
    }

    @Test
    fun `GIVEN a malformed recipient WHEN encoded THEN there is no call instead of a burn address`() {
        // Act
        val actual = codec.encode(
            JointTransferIntent.Erc20(tokenContract = TOKEN, recipient = "0xdead", amountWei = AMOUNT),
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN an encoded token transfer WHEN decoded THEN the intent comes back`() {
        // Arrange
        val intent = JointTransferIntent.Erc20(tokenContract = TOKEN, recipient = RECIPIENT, amountWei = AMOUNT)
        val call = requireNotNull(codec.encode(intent))

        // Act
        val actual = codec.decode(safeTx(to = call.to, value = call.value, data = call.data))

        // Assert
        assertThat(actual).isEqualTo(intent)
    }

    @Test
    fun `GIVEN a plain call with a value WHEN decoded THEN it is the coin transfer to the call target`() {
        // Act
        val actual = codec.decode(safeTx(to = RECIPIENT, value = AMOUNT, data = "0x"))

        // Assert
        assertThat(actual).isEqualTo(JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT))
    }

    @Test
    fun `GIVEN an upper-case hex prefix on empty calldata WHEN decoded THEN it is still the coin transfer`() {
        // Act
        val actual = codec.decode(safeTx(to = RECIPIENT, value = AMOUNT, data = "0X"))

        // Assert
        assertThat(actual).isEqualTo(JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT))
    }

    @Test
    fun `GIVEN payloads outside the two allowed shapes WHEN decoded THEN there is no transfer`() {
        // Arrange
        val transfer = requireNotNull(codec.encode(erc20())).data
        val approve = "0x095ea7b3" + transfer.removePrefix(prefix = "0xa9059cbb")
        val truncated = transfer.dropLast(n = 2)
        val oversized = transfer + "00".repeat(n = 1024)
        val zeroValuePlainCall = safeTx(to = RECIPIENT, value = BigInteger.ZERO, data = "0x")
        val tokenTransferMovingValue = safeTx(to = TOKEN, value = AMOUNT, data = transfer)

        // Act
        val actual = listOf(
            codec.decode(safeTx(to = TOKEN, value = BigInteger.ZERO, data = approve)),
            codec.decode(safeTx(to = TOKEN, value = BigInteger.ZERO, data = truncated)),
            codec.decode(safeTx(to = TOKEN, value = BigInteger.ZERO, data = oversized)),
            codec.decode(zeroValuePlainCall),
            codec.decode(tokenTransferMovingValue),
        )

        // Assert
        assertThat(actual).containsExactly(null, null, null, null, null)
    }

    /**
     * Nothing decodes the recipient of a plain call — it is the call target itself, so a target the schema cannot
     * hold would otherwise be shown as a transfer to a meaningless address and fail only at signing.
     */
    @Test
    fun `GIVEN a plain call to a malformed target WHEN decoded THEN there is no transfer`() {
        // Arrange
        val tooShort = safeTx(to = RECIPIENT.dropLast(n = 1), value = AMOUNT, data = "0x")
        val notHex = safeTx(to = "0x" + "z".repeat(n = 40), value = AMOUNT, data = "0x")
        val empty = safeTx(to = "", value = AMOUNT, data = "0x")

        // Act
        val actual = listOf(codec.decode(tooShort), codec.decode(notHex), codec.decode(empty))

        // Assert
        assertThat(actual).containsExactly(null, null, null)
    }

    private fun erc20() = JointTransferIntent.Erc20(
        tokenContract = TOKEN,
        recipient = RECIPIENT,
        amountWei = AMOUNT,
    )

    private fun safeTx(to: String, value: BigInteger, data: String) = SafeTx(
        to = to,
        value = value,
        data = data,
        operation = SafeTx.OPERATION_CALL,
        safeTxGas = BigInteger.ZERO,
        baseGas = BigInteger.ZERO,
        gasPrice = BigInteger.ZERO,
        gasToken = ZERO_ADDRESS,
        refundReceiver = ZERO_ADDRESS,
        nonce = BigInteger.ZERO,
    )

    private companion object {
        const val TOKEN = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
        val AMOUNT: BigInteger = BigInteger("500000000000000000")
    }
}