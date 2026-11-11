package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySendCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplySwapCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldModuleUpgradeUnwrapTest {

    private val send = EthereumYieldSupplySendCallData(
        tokenContractAddress = "0xtoken",
        destinationAddress = "0xrecipient",
        amount = mockk(relaxed = true),
    )

    private val swap = EthereumYieldSupplySwapCallData(
        tokenIn = "0xtoken",
        amountIn = BigInteger.TEN,
        target = "0xrouter",
        spender = "0xspender",
        swapData = byteArrayOf(0x01),
    )

    @Test
    fun `GIVEN send wrapped with module upgrade WHEN unwrapYieldSendUpgrade THEN send runs bare`() {
        // Arrange
        val transaction = transactionWith(send.withModuleUpgrade())

        // Act
        val actual = transaction.unwrapYieldSendUpgrade()

        // Assert
        assertThat(actual.callData).isSameInstanceAs(send)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN call other than upgraded send WHEN unwrapYieldSendUpgrade THEN transaction is unchanged`(
        callData: SmartContractCallData,
    ) {
        // Arrange
        val transaction = transactionWith(callData)

        // Act
        val actual = transaction.unwrapYieldSendUpgrade()

        // Assert
        assertThat(actual).isSameInstanceAs(transaction)
    }

    private fun provideTestModels(): List<SmartContractCallData> = listOf(
        swap.withModuleUpgrade(),
        send,
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

    private val TransactionData.callData: SmartContractCallData?
        get() = ((this as TransactionData.Uncompiled).extras as EthereumTransactionExtras).callData
}