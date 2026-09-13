package com.tangem.lib.crypto

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.lib.crypto.BlockchainFeeUtils.patchIntegratedApprovalPriorityFee
import com.tangem.lib.crypto.BlockchainFeeUtils.patchTransactionFeeForSwap
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class BlockchainFeeUtilsTest {

    private val gasLimit = BigInteger.valueOf(50_000)
    private val gasPrice = BigInteger.valueOf(20_000_000_000)

    @Test
    fun `GIVEN eip1559 fee WHEN patch approval fee THEN amount is recomputed from increased gas price`() {
        // Arrange
        val fee = TransactionFee.Single(
            normal = Fee.Ethereum.EIP1559(
                amount = Amount(value = BigDecimal("0.001"), blockchain = Blockchain.Ethereum),
                gasLimit = gasLimit,
                maxFeePerGas = gasPrice,
                priorityFee = BigInteger.valueOf(5_000_000_000),
            ),
        )

        // Act
        val actual = fee.patchIntegratedApprovalPriorityFee(increaseBy = 115).normal as Fee.Ethereum.EIP1559

        // Assert
        assertThat(actual.maxFeePerGas).isEqualTo(BigInteger.valueOf(23_000_000_000))
        assertThat(actual.priorityFee).isEqualTo(BigInteger.valueOf(5_750_000_000))
        assertThat(actual.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.00115"))
    }

    @Test
    fun `GIVEN legacy fee WHEN patch approval fee THEN amount and gasPrice grow by the same percent`() {
        // Arrange
        val fee = TransactionFee.Single(
            normal = Fee.Ethereum.Legacy(
                amount = Amount(value = BigDecimal("0.001"), blockchain = Blockchain.Ethereum),
                gasLimit = gasLimit,
                gasPrice = gasPrice,
            ),
        )

        // Act
        val actual = fee.patchIntegratedApprovalPriorityFee(increaseBy = 150).normal as Fee.Ethereum.Legacy

        // Assert
        assertThat(actual.gasPrice).isEqualTo(BigInteger.valueOf(30_000_000_000))
        assertThat(actual.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.0015"))
    }

    @Test
    fun `GIVEN eip1559 fee WHEN patch gas limit for swap THEN amount grows with gas limit`() {
        // Arrange
        val fee = TransactionFee.Single(
            normal = Fee.Ethereum.EIP1559(
                amount = Amount(value = BigDecimal("0.001"), blockchain = Blockchain.Ethereum),
                gasLimit = gasLimit,
                maxFeePerGas = gasPrice,
                priorityFee = BigInteger.valueOf(5_000_000_000),
            ),
        )

        // Act
        val actual = fee.patchTransactionFeeForSwap(increaseBy = 105).normal as Fee.Ethereum.EIP1559

        // Assert
        assertThat(actual.gasLimit).isEqualTo(BigInteger.valueOf(52_500))
        assertThat(actual.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.00105"))
    }
}