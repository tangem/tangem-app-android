package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySwapCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.transaction.models.GaslessYieldWithdraw
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigInteger

/**
 * Unit tests for [CreateAndSendEvmGaslessTransactionUseCase.unwrapFeePlanIfNeeded] — the batch upgrades the yield
 * module at most once.
 */
internal class CreateAndSendEvmGaslessUnwrapFeeTest {

    private val feeToken: CryptoCurrency.Token = mockk()
    private val fee: Fee.Ethereum.TokenCurrency = mockk()

    private val withdraw = object : SmartContractCallData {
        override val methodId: String = "0xf3fef3a3"
        override val data: ByteArray = byteArrayOf(0x12, 0x34)
        override fun validate(blockchain: Blockchain) = true
    }

    private val swap = EthereumYieldSupplySwapCallData(
        tokenIn = "0xtoken",
        amountIn = BigInteger.TEN,
        target = "0xrouter",
        spender = "0xspender",
        swapData = byteArrayOf(0x01),
    )

    private val send = EthereumYieldSupplySendCallData(
        tokenContractAddress = "0xtoken",
        destinationAddress = "0xrecipient",
        amount = mockk(relaxed = true),
    )

    @Test
    fun `GIVEN user call upgrading the module WHEN unwrapFeePlanIfNeeded THEN withdraw drops its own upgrade`() {
        // Arrange
        val plan = planWithWithdraw(withdraw.withModuleUpgrade())

        // Act
        val actual = CreateAndSendEvmGaslessTransactionUseCase.unwrapFeePlanIfNeeded(
            plan = plan,
            transactionDataList = listOf(transactionWith(swap.withModuleUpgrade())),
        )

        // Assert
        assertThat(actual).isEqualTo(planWithWithdraw(withdraw))
    }

    @Test
    fun `GIVEN no user call upgrading the module WHEN unwrapFeePlanIfNeeded THEN withdraw keeps its upgrade`() {
        // Arrange
        val plan = planWithWithdraw(withdraw.withModuleUpgrade())

        // Act
        val actual = CreateAndSendEvmGaslessTransactionUseCase.unwrapFeePlanIfNeeded(
            plan = plan,
            transactionDataList = listOf(transactionWith(send)),
        )

        // Assert
        assertThat(actual).isSameInstanceAs(plan)
    }

    private fun planWithWithdraw(callData: SmartContractCallData) = GaslessFeePlan.TokenPay(
        feeToken = feeToken,
        fee = fee,
        yieldWithdraw = GaslessYieldWithdraw(
            amount = BigInteger.TEN,
            callData = callData,
            moduleAddress = "0xmodule",
        ),
    )

    private fun SmartContractCallData.withModuleUpgrade() = EthereumYieldSupplyUpgradeToAndCallCallData(
        newImplementation = "0x0000000000000000000000000000000000000001",
        innerCallData = this,
    )

    private fun transactionWith(callData: SmartContractCallData) = TransactionData.Uncompiled(
        amount = mockk(relaxed = true),
        fee = null,
        sourceAddress = "0xsource",
        destinationAddress = "0xmodule",
        extras = EthereumTransactionExtras(callData = callData),
    )
}