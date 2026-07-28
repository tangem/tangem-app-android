package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.transaction.models.GaslessTransactionData
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class Eip712TypedDataBuilderTest {

    @Test
    fun `build emits GaslessTransaction primary type with per-call gasLimit in type and message`() {
        // Arrange
        val gaslessTransaction = GaslessTransactionData(
            transaction = GaslessTransactionData.Transaction(
                to = "0xaaa",
                value = BigInteger.ZERO,
                gasLimit = BigInteger.valueOf(120_000),
                data = byteArrayOf(0x12, 0x34),
            ),
            fee = GaslessTransactionData.Fee(
                feeToken = "0xtoken",
                maxTokenFee = BigInteger.TEN,
                coinPriceInToken = BigInteger.ONE,
                feeTransferGasLimit = BigInteger.valueOf(60_000),
                baseGas = BigInteger.valueOf(60_000),
                feeReceiver = "0xrecv",
            ),
            nonce = BigInteger.ZERO,
        )

        // Act
        val json = JSONObject(
            Eip712TypedDataBuilder.build(gaslessTransaction, chainId = 137, verifyingContract = "0xuser"),
        )

        // Assert
        assertThat(json.getString("primaryType")).isEqualTo("GaslessTransaction")

        // v2: the single transaction carries its per-call gasLimit in the message
        val txMessage = json.getJSONObject("message").getJSONObject("transaction")
        assertThat(txMessage.getString("gasLimit")).isEqualTo("120000")

        // v2: the Transaction struct adds gasLimit between value and data (order defines the EIP-712 typehash)
        val txType = json.getJSONObject("types").getJSONArray("Transaction")
        val txTypeFields = (0 until txType.length()).map { txType.getJSONObject(it).getString("name") }
        assertThat(txTypeFields).containsExactly("to", "value", "gasLimit", "data").inOrder()

        // Domain is unchanged between v1/v2; verifyingContract is the user's EOA address
        val domain = json.getJSONObject("domain")
        assertThat(domain.getString("name")).isEqualTo("Tangem7702GaslessExecutor")
        assertThat(domain.getString("version")).isEqualTo("1")
        assertThat(domain.getString("verifyingContract")).isEqualTo("0xuser")
    }

    @Test
    fun `build with includeGasLimit false omits gasLimit reproducing the v1 typehash`() {
        // Arrange
        val gaslessTransaction = GaslessTransactionData(
            transaction = GaslessTransactionData.Transaction(
                to = "0xaaa",
                value = BigInteger.ZERO,
                gasLimit = BigInteger.valueOf(120_000),
                data = byteArrayOf(0x12, 0x34),
            ),
            fee = GaslessTransactionData.Fee(
                feeToken = "0xtoken",
                maxTokenFee = BigInteger.TEN,
                coinPriceInToken = BigInteger.ONE,
                feeTransferGasLimit = BigInteger.valueOf(60_000),
                baseGas = BigInteger.valueOf(60_000),
                feeReceiver = "0xrecv",
            ),
            nonce = BigInteger.ZERO,
        )

        // Act — v1 mode (feature flag off)
        val json = JSONObject(
            Eip712TypedDataBuilder.build(
                gaslessTransaction = gaslessTransaction,
                chainId = 137,
                verifyingContract = "0xuser",
                includeGasLimit = false,
            ),
        )

        // Assert: the Transaction struct is the legacy {to, value, data} — gasLimit drives the typehash, so its
        // absence reproduces exactly the v1 hash the current develop signs.
        val txType = json.getJSONObject("types").getJSONArray("Transaction")
        val txTypeFields = (0 until txType.length()).map { txType.getJSONObject(it).getString("name") }
        assertThat(txTypeFields).containsExactly("to", "value", "data").inOrder()

        // and the message carries no gasLimit
        val txMessage = json.getJSONObject("message").getJSONObject("transaction")
        assertThat(txMessage.has("gasLimit")).isFalse()
    }
}