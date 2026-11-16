package com.tangem.domain.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class CombineTransactionFeesTest {

    @Test
    fun `GIVEN Choosable approval and swap WHEN combine THEN each bucket sums amount and gas limit`() {
        // Arrange
        val approval = choosable(min = 1, normal = 2, priority = 3, gas = 21_000)
        val swap = choosable(min = 10, normal = 20, priority = 30, gas = 50_000)

        // Act
        val result = combineTransactionFees(approvalFee = approval, swapFee = swap)

        // Assert
        assertThat(result).isEqualTo(choosable(min = 11, normal = 22, priority = 33, gas = 71_000))
    }

    @Test
    fun `GIVEN Single approval and Choosable swap WHEN combine THEN Single sums normal fees`() {
        // Arrange
        val approval = TransactionFee.Single(normal = eip1559(value = 2, gas = 21_000))
        val swap = choosable(min = 10, normal = 20, priority = 30, gas = 50_000)

        // Act
        val result = combineTransactionFees(approvalFee = approval, swapFee = swap)

        // Assert
        assertThat(result).isEqualTo(TransactionFee.Single(normal = eip1559(value = 22, gas = 71_000)))
    }

    @Test
    fun `GIVEN both Single WHEN combine THEN Single sums normal fees`() {
        // Arrange
        val approval = TransactionFee.Single(normal = eip1559(value = 5, gas = 21_000))
        val swap = TransactionFee.Single(normal = eip1559(value = 7, gas = 30_000))

        // Act
        val result = combineTransactionFees(approvalFee = approval, swapFee = swap)

        // Assert
        assertThat(result).isEqualTo(TransactionFee.Single(normal = eip1559(value = 12, gas = 51_000)))
    }

    @Test
    fun `GIVEN Legacy fees WHEN combine THEN sums amount and gas limit`() {
        // Arrange
        val approval = TransactionFee.Single(normal = legacy(value = 2, gas = 21_000))
        val swap = TransactionFee.Single(normal = legacy(value = 20, gas = 50_000))

        // Act
        val result = combineTransactionFees(approvalFee = approval, swapFee = swap)

        // Assert
        assertThat(result).isEqualTo(TransactionFee.Single(normal = legacy(value = 22, gas = 71_000)))
    }

    @Test
    fun `GIVEN non-EVM fees WHEN combine THEN returns swap fee alone`() {
        // Arrange
        val approval = TransactionFee.Single(normal = Fee.Common(ethAmount(value = 2)))
        val swap = TransactionFee.Single(normal = Fee.Common(ethAmount(value = 20)))

        // Act
        val result = combineTransactionFees(approvalFee = approval, swapFee = swap)

        // Assert
        assertThat(result).isEqualTo(swap)
    }

    private fun choosable(min: Int, normal: Int, priority: Int, gas: Long) = TransactionFee.Choosable(
        minimum = eip1559(value = min, gas = gas),
        normal = eip1559(value = normal, gas = gas),
        priority = eip1559(value = priority, gas = gas),
    )

    private fun eip1559(value: Int, gas: Long) = Fee.Ethereum.EIP1559(
        amount = ethAmount(value),
        gasLimit = BigInteger.valueOf(gas),
        maxFeePerGas = BigInteger.ONE,
        priorityFee = BigInteger.ONE,
    )

    private fun legacy(value: Int, gas: Long) = Fee.Ethereum.Legacy(
        amount = ethAmount(value),
        gasLimit = BigInteger.valueOf(gas),
        gasPrice = BigInteger.ONE,
    )

    private fun ethAmount(value: Int) = Amount(currencySymbol = "ETH", value = BigDecimal(value), decimals = 18)
}