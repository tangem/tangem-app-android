package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for [CreateAndSendGaslessTransactionUseCase.getDestinationAddress] — resolves the on-chain
 * `to` of the user's main gasless sub-call.
 *
 * Regression guard: a yield-supply send must target the user's yield MODULE (the contract that
 * runs `send(token, dest, amount)`), not the transfer recipient. Targeting the recipient reverts the whole
 * batch with GAS_ESTIMATION_FAILED / require(false).
 */
internal class CreateAndSendGaslessDestinationAddressTest {

    private val module = "0xmodule"
    private val recipient = "0xrecipient"
    private val tokenContract = "0xtokencontract"

    private fun uncompiled(
        destinationAddress: String,
        extras: EthereumTransactionExtras?,
        contractAddress: String?,
    ) = TransactionData.Uncompiled(
        amount = mockk(relaxed = true),
        fee = null,
        sourceAddress = "0xsource",
        destinationAddress = destinationAddress,
        extras = extras,
        contractAddress = contractAddress,
    )

    @Test
    fun `GIVEN yield-supply send WHEN getDestinationAddress THEN returns module not recipient`() {
        // Arrange — destinationAddress is patched to the yield module; the recipient lives inside the callData
        val yieldCallData = EthereumYieldSupplySendCallData(
            tokenContractAddress = tokenContract,
            destinationAddress = recipient,
            amount = mockk(relaxed = true),
        )
        val txData = uncompiled(
            destinationAddress = module,
            extras = EthereumTransactionExtras(callData = yieldCallData),
            contractAddress = tokenContract,
        )

        // Act
        val to = CreateAndSendGaslessTransactionUseCase.getDestinationAddress(txData)

        // Assert
        assertThat(to).isEqualTo(module)
    }

    @Test
    fun `GIVEN ERC20 transfer WHEN getDestinationAddress THEN returns token contract`() {
        // Arrange — a non-yield callData; `to` must be the token contract, not the recipient
        val erc20CallData = object : SmartContractCallData {
            override val methodId = "0xa9059cbb"
            override val data = byteArrayOf(0x01)
            override fun validate(blockchain: Blockchain) = true
        }
        val txData = uncompiled(
            destinationAddress = recipient,
            extras = EthereumTransactionExtras(callData = erc20CallData),
            contractAddress = tokenContract,
        )

        // Act
        val to = CreateAndSendGaslessTransactionUseCase.getDestinationAddress(txData)

        // Assert
        assertThat(to).isEqualTo(tokenContract)
    }

    @Test
    fun `GIVEN non-yield tx without contract address WHEN getDestinationAddress THEN throws`() {
        // Arrange
        val txData = uncompiled(
            destinationAddress = recipient,
            extras = null,
            contractAddress = null,
        )

        // Act & Assert
        assertThrows<IllegalStateException> {
            CreateAndSendGaslessTransactionUseCase.getDestinationAddress(txData)
        }
    }
}