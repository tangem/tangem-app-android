package com.tangem.domain.transaction.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class GaslessBatchTransactionDataTest {
    @Test
    fun `holds transactions fee and nonce`() {
        val tx = GaslessTransactionData.Transaction(
            to = "0xabc", value = BigInteger.ZERO, gasLimit = BigInteger.valueOf(120_000), data = byteArrayOf(1),
        )
        val withdraw = GaslessTransactionData.Transaction(
            to = "0xdef", value = BigInteger.ZERO, gasLimit = BigInteger.valueOf(150_000), data = byteArrayOf(2),
        )
        val fee = GaslessTransactionData.Fee(
            feeToken = "0xtoken", maxTokenFee = BigInteger.TEN, coinPriceInToken = BigInteger.ONE,
            feeTransferGasLimit = BigInteger.valueOf(100), baseGas = BigInteger.valueOf(60000), feeReceiver = "0xrecv",
        )
        val batch = GaslessBatchTransactionData(transactions = listOf(tx, withdraw), fee = fee, nonce = BigInteger.ZERO)

        assertThat(batch.transactions).hasSize(2)
        assertThat(batch.transactions[1]).isEqualTo(withdraw)
    }
}