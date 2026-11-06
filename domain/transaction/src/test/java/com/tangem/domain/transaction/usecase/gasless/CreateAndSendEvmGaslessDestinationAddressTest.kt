package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyDeployCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyInitTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyReactivateTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySwapCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyWithdrawCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.MerklClaimCallData
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

/**
 * Unit tests for [CreateAndSendEvmGaslessTransactionUseCase.getDestinationAddress] — resolves the on-chain
 * `to` of the user's main gasless sub-call.
 *
 * Regression guard: a yield-supply send must target the user's yield MODULE (the contract that
 * runs `send(token, dest, amount)`), not the transfer recipient. Targeting the recipient reverts the whole
 * batch with GAS_ESTIMATION_FAILED / require(false).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateAndSendEvmGaslessDestinationAddressTest {

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
        val to = CreateAndSendEvmGaslessTransactionUseCase.getDestinationAddress(txData)

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
        val to = CreateAndSendEvmGaslessTransactionUseCase.getDestinationAddress(txData)

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
            CreateAndSendEvmGaslessTransactionUseCase.getDestinationAddress(txData)
        }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN yield mode call data WHEN getDestinationAddress THEN returns transaction destination`(
        model: YieldModeCallModel,
    ) {
        // Arrange
        val txData = uncompiled(
            destinationAddress = model.destination,
            extras = EthereumTransactionExtras(callData = model.callData),
            contractAddress = tokenContract,
        )

        // Act
        val to = CreateAndSendEvmGaslessTransactionUseCase.getDestinationAddress(txData)

        // Assert
        assertThat(to).isEqualTo(model.destination)
    }

    private fun provideTestModels(): List<YieldModeCallModel> {
        val enter = EthereumYieldSupplyEnterCallData(tokenContractAddress = tokenContract, amount = BigInteger.TEN)
        return listOf(
            YieldModeCallModel(callData = enter, destination = module),
            YieldModeCallModel(
                callData = EthereumYieldSupplyUpgradeToAndCallCallData(
                    newImplementation = "0ximpl",
                    innerCallData = enter,
                ),
                destination = module,
            ),
            YieldModeCallModel(
                callData = EthereumYieldSupplyDeployCallData(
                    address = "0xsource",
                    tokenContractAddress = tokenContract,
                    maxNetworkFee = mockk(relaxed = true),
                ),
                destination = "0xfactory",
            ),
            YieldModeCallModel(
                callData = ApprovalERC20TokenCallData(spenderAddress = module, amount = null),
                destination = tokenContract,
            ),
            YieldModeCallModel(
                callData = EthereumYieldSupplyInitTokenCallData(
                    tokenContractAddress = tokenContract,
                    maxNetworkFee = mockk(relaxed = true),
                ),
                destination = module,
            ),
            YieldModeCallModel(
                callData = EthereumYieldSupplyReactivateTokenCallData(
                    tokenContractAddress = tokenContract,
                    maxNetworkFee = mockk(relaxed = true),
                ),
                destination = module,
            ),
            YieldModeCallModel(
                callData = EthereumYieldSupplyExitCallData(tokenContractAddress = tokenContract),
                destination = module,
            ),
            YieldModeCallModel(
                callData = EthereumYieldSupplyWithdrawCallData(
                    tokenContractAddress = tokenContract,
                    amount = mockk(relaxed = true),
                ),
                destination = module,
            ),
            YieldModeCallModel(callData = MerklClaimCallData(data = byteArrayOf(0x01)), destination = module),
            YieldModeCallModel(
                callData = EthereumYieldSupplySwapCallData(
                    tokenIn = tokenContract,
                    amountIn = BigInteger.ONE,
                    target = "0xrouter",
                    spender = "0xrouter",
                    swapData = byteArrayOf(0x01),
                ),
                destination = module,
            ),
            YieldModeCallModel(
                callData = EthereumYieldSupplyUpgradeToAndCallCallData(
                    newImplementation = "0ximpl",
                    innerCallData = EthereumYieldSupplySendCallData(
                        tokenContractAddress = tokenContract,
                        destinationAddress = recipient,
                        amount = mockk(relaxed = true),
                    ),
                ),
                destination = module,
            ),
        )
    }

    data class YieldModeCallModel(val callData: SmartContractCallData, val destination: String)
}