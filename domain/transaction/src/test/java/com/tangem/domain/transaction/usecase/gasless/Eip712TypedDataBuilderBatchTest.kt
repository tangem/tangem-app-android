package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.transaction.models.GaslessBatchTransactionData
import com.tangem.domain.transaction.models.GaslessTransactionData
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class Eip712TypedDataBuilderBatchTest {

    @Test
    fun `buildBatch emits GaslessBatchTransaction primary type with transactions array`() {
        val tx = GaslessTransactionData.Transaction(
            to = "0xaaa", value = BigInteger.ZERO, gasLimit = BigInteger.valueOf(120_000), data = byteArrayOf(0x12),
        )
        val withdraw = GaslessTransactionData.Transaction(
            to = "0xbbb", value = BigInteger.ZERO, gasLimit = BigInteger.valueOf(150_000), data = byteArrayOf(0x34),
        )
        val fee = GaslessTransactionData.Fee(
            feeToken = "0xtoken", maxTokenFee = BigInteger.TEN, coinPriceInToken = BigInteger.ONE,
            feeTransferGasLimit = BigInteger.valueOf(100), baseGas = BigInteger.valueOf(60000), feeReceiver = "0xrecv",
        )
        val batch = GaslessBatchTransactionData(listOf(tx, withdraw), fee, BigInteger.ZERO)

        val json = JSONObject(Eip712TypedDataBuilder.buildBatch(batch, chainId = 1, verifyingContract = "0xuser"))

        assertThat(json.getString("primaryType")).isEqualTo("GaslessBatchTransaction")
        val message = json.getJSONObject("message")
        assertThat(message.getJSONArray("transactions").length()).isEqualTo(2)
        assertThat(message.getJSONArray("transactions").getJSONObject(1).getString("to")).isEqualTo("0xbbb")
        // v2: each sub-call carries its per-call gasLimit in the message
        assertThat(message.getJSONArray("transactions").getJSONObject(1).getString("gasLimit")).isEqualTo("150000")
        val types = json.getJSONObject("types").getJSONArray("GaslessBatchTransaction")
        assertThat(types.getJSONObject(0).getString("type")).isEqualTo("Transaction[]")
        // v2: the Transaction struct adds gasLimit between value and data
        val txType = json.getJSONObject("types").getJSONArray("Transaction")
        val txTypeFields = (0 until txType.length()).map { txType.getJSONObject(it).getString("name") }
        assertThat(txTypeFields).containsExactly("to", "value", "gasLimit", "data").inOrder()
    }
}