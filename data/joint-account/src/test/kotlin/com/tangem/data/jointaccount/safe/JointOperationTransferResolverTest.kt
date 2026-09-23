package com.tangem.data.jointaccount.safe

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class JointOperationTransferResolverTest {

    private val resolver = JointOperationTransferResolver(payloadCodec = DefaultJointTransferPayloadCodec())

    private val network: Network = mockk()
    private val otherNetwork: Network = mockk()
    private val coin: CryptoCurrency.Coin = mockk { every { network } returns this@JointOperationTransferResolverTest.network }
    private val token: CryptoCurrency.Token = mockk {
        every { network } returns this@JointOperationTransferResolverTest.network
        every { contractAddress } returns TOKEN
    }
    private val otherNetworkCoin: CryptoCurrency.Coin = mockk { every { network } returns otherNetwork }

    @Test
    fun `GIVEN a call with empty calldata WHEN resolved THEN it is the coin transfer to the call target`() {
        // Act
        val actual = resolver.resolve(
            safeTx = safeTx(),
            currencies = listOf(token, coin),
            network = network,
            safeAddress = SAFE,
        )

        // Assert
        assertThat(actual).isEqualTo(
            JointOperationTransfer(
                currency = coin,
                intent = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            ),
        )
    }

    @Test
    fun `GIVEN an upper-case hex prefix on empty calldata WHEN resolved THEN it is still the coin transfer`() {
        // Act
        val actual = resolver.resolve(
            safeTx = safeTx(data = "0X"),
            currencies = listOf(coin),
            network = network,
            safeAddress = SAFE,
        )

        // Assert
        assertThat(actual).isEqualTo(
            JointOperationTransfer(
                currency = coin,
                intent = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT),
            ),
        )
    }

    @Test
    fun `GIVEN a call the Safe makes to itself WHEN resolved THEN nothing is shown`() {
        // Act
        val actual = resolver.resolve(
            safeTx = safeTx(to = SAFE.uppercase().replaceFirst("0X", "0x")),
            currencies = listOf(coin, token),
            network = network,
            safeAddress = SAFE,
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a plain call with no value WHEN resolved THEN nothing is shown`() {
        // Act
        val actual = resolver.resolve(
            safeTx = safeTx(value = BigInteger.ZERO),
            currencies = listOf(coin, token),
            network = network,
            safeAddress = SAFE,
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a transfer call to a held token WHEN resolved THEN it is the token transfer to the decoded recipient`() {
        // Arrange
        val safeTx = safeTx(
            to = TOKEN.uppercase().replaceFirst("0X", "0x"),
            value = BigInteger.ZERO,
            data = transferCalldata(recipient = RECIPIENT, amount = AMOUNT),
        )

        // Act
        val actual = resolver.resolve(
            safeTx = safeTx,
            currencies = listOf(coin, token),
            network = network,
            safeAddress = SAFE,
        )

        // Assert
        assertThat(actual).isEqualTo(
            JointOperationTransfer(
                currency = token,
                intent = JointTransferIntent.Erc20(tokenContract = safeTx.to, recipient = RECIPIENT, amountWei = AMOUNT),
            ),
        )
    }

    @Test
    fun `GIVEN the account holds no coin of the network WHEN a coin call is resolved THEN nothing is shown`() {
        // Act
        val actual = resolver.resolve(
            safeTx = safeTx(),
            currencies = listOf(otherNetworkCoin, token),
            network = network,
            safeAddress = SAFE,
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a transfer call to an unknown token WHEN resolved THEN nothing is shown`() {
        // Arrange
        val safeTx = safeTx(
            to = "0x" + "9".repeat(40),
            value = BigInteger.ZERO,
            data = transferCalldata(recipient = RECIPIENT, amount = AMOUNT),
        )

        // Act
        val actual = resolver.resolve(
            safeTx = safeTx,
            currencies = listOf(coin, token),
            network = network,
            safeAddress = SAFE,
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a transfer call that also carries value WHEN resolved THEN nothing is shown`() {
        // Arrange
        val safeTx = safeTx(
            to = TOKEN,
            value = BigInteger.ONE,
            data = transferCalldata(recipient = RECIPIENT, amount = AMOUNT),
        )

        // Act
        val actual = resolver.resolve(
            safeTx = safeTx,
            currencies = listOf(coin, token),
            network = network,
            safeAddress = SAFE,
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN unknown calldata or a delegate call WHEN resolved THEN nothing is shown`() {
        // Arrange
        val approve = safeTx(to = TOKEN, value = BigInteger.ZERO, data = "0x095ea7b3" + "0".repeat(128))
        val delegateCall = safeTx(operation = 1)

        // Act
        val actual = listOf(approve, delegateCall).map {
            resolver.resolve(
                safeTx = it,
                currencies = listOf(coin, token),
                network = network,
                safeAddress = SAFE,
            )
        }

        // Assert
        assertThat(actual).containsExactly(null, null)
    }

    private fun safeTx(
        to: String = RECIPIENT,
        value: BigInteger = AMOUNT,
        data: String = "0x",
        operation: Int = SafeTx.OPERATION_CALL,
    ): SafeTx = SafeTx(
        to = to,
        value = value,
        data = data,
        operation = operation,
        safeTxGas = BigInteger("150000"),
        baseGas = BigInteger("110000"),
        gasPrice = BigInteger("60000000000"),
        gasToken = "0x0000000000000000000000000000000000000000",
        refundReceiver = "0x0000000000000000000000000000000000000000",
        nonce = BigInteger.ZERO,
    )

    private fun transferCalldata(recipient: String, amount: java.math.BigInteger): String = requireNotNull(
        DefaultJointTransferPayloadCodec().encode(
            com.tangem.domain.jointaccount.model.JointTransferIntent.Erc20(
                tokenContract = "0x" + "1".repeat(n = 40),
                recipient = recipient,
                amountWei = amount,
            ),
        ),
    ).data

    private companion object {
        const val SAFE = "0x5afe000000000000000000000000000000000001"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val TOKEN = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        val AMOUNT: BigInteger = BigInteger("2500000")
    }
}