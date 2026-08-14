package com.tangem.domain.transaction.usecase.gasless

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

/** Unit tests for [CreateAndSendGaslessTransactionUseCase.validateTransactionRecipients]. */
internal class CreateAndSendGaslessRecipientValidationTest {

    private val blockchain = Blockchain.Ethereum
    private val tokenContract = "0xdac17f958d2ee523a2206206994597c13d831ec7"
    private val recipient = "0xfc9013965447f804042a03ae4b98130a8c300a2f"
    private val yieldModule = "0x3a1f7e2c9b4d5e6f80912a3b4c5d6e7f8091a2b3"
    private val zeroAddress = "0x0000000000000000000000000000000000000000"
    private val deadAddress = "0x000000000000000000000000000000000000dEaD"

    private val amount = Amount(
        token = Token(symbol = "USDT", contractAddress = tokenContract, decimals = 6),
        value = BigDecimal("994"),
    )

    private fun uncompiled(destinationAddress: String, callData: SmartContractCallData?) = TransactionData.Uncompiled(
        amount = amount,
        fee = null,
        sourceAddress = "0x7f56aab66955bc02cc6b2870d4cddc12b0221c55",
        destinationAddress = destinationAddress,
        extras = callData?.let { EthereumTransactionExtras(callData = it) },
        contractAddress = tokenContract,
    )

    @Test
    fun `GIVEN valid ERC20 transfer WHEN validateTransactionRecipients THEN passes`() {
        // Arrange
        val txData = uncompiled(
            destinationAddress = recipient,
            callData = TransferERC20TokenCallData(destination = recipient, amount = amount),
        )

        // Act & Assert — no exception
        CreateAndSendGaslessTransactionUseCase.validateTransactionRecipients(blockchain, txData)
    }

    @Test
    fun `GIVEN blank recipient WHEN validateTransactionRecipients THEN throws`() {
        // Arrange
        val txData = uncompiled(
            destinationAddress = "",
            callData = TransferERC20TokenCallData(destination = "", amount = amount),
        )

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            CreateAndSendGaslessTransactionUseCase.validateTransactionRecipients(blockchain, txData)
        }
    }

    @Test
    fun `GIVEN zero address recipient WHEN validateTransactionRecipients THEN throws`() {
        // Arrange
        val txData = uncompiled(
            destinationAddress = zeroAddress,
            callData = TransferERC20TokenCallData(destination = zeroAddress, amount = amount),
        )

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            CreateAndSendGaslessTransactionUseCase.validateTransactionRecipients(blockchain, txData)
        }
    }

    @Test
    fun `GIVEN zero address inside call data only WHEN validateTransactionRecipients THEN throws`() {
        // Arrange
        val txData = uncompiled(
            destinationAddress = tokenContract,
            callData = TransferERC20TokenCallData(destination = zeroAddress, amount = amount),
        )

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            CreateAndSendGaslessTransactionUseCase.validateTransactionRecipients(blockchain, txData)
        }
    }

    @Test
    fun `GIVEN dead address recipient WHEN validateTransactionRecipients THEN throws`() {
        // Arrange — the dead address is well-formed and non-zero, so every other check accepts it
        val txData = uncompiled(
            destinationAddress = deadAddress,
            callData = TransferERC20TokenCallData(destination = deadAddress, amount = amount),
        )

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            CreateAndSendGaslessTransactionUseCase.validateTransactionRecipients(blockchain, txData)
        }
    }

    @Test
    fun `GIVEN dead address inside yield supply send call data WHEN validateTransactionRecipients THEN throws`() {
        // Arrange — for a yield-supply send the recipient lives in the call data
        val txData = uncompiled(
            destinationAddress = yieldModule,
            callData = EthereumYieldSupplySendCallData(
                tokenContractAddress = tokenContract,
                destinationAddress = deadAddress,
                amount = amount,
            ),
        )

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            CreateAndSendGaslessTransactionUseCase.validateTransactionRecipients(blockchain, txData)
        }
    }

    @Test
    fun `GIVEN valid yield supply send WHEN validateTransactionRecipients THEN passes`() {
        // Arrange
        val txData = uncompiled(
            destinationAddress = yieldModule,
            callData = EthereumYieldSupplySendCallData(
                tokenContractAddress = tokenContract,
                destinationAddress = recipient,
                amount = amount,
            ),
        )

        // Act & Assert — no exception
        CreateAndSendGaslessTransactionUseCase.validateTransactionRecipients(blockchain, txData)
    }

    @Test
    fun `GIVEN missing call data WHEN validateTransactionRecipients THEN throws`() {
        // Arrange
        val txData = uncompiled(destinationAddress = recipient, callData = null)

        // Act & Assert
        assertThrows<IllegalStateException> {
            CreateAndSendGaslessTransactionUseCase.validateTransactionRecipients(blockchain, txData)
        }
    }
}