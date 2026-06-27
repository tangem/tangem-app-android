package com.tangem.domain.transaction.usecase.gasless

import com.tangem.common.extensions.toHexString
import com.tangem.domain.transaction.models.GaslessBatchTransactionData
import com.tangem.domain.transaction.models.GaslessTransactionData
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builder for creating EIP-712 typed data JSON for gasless transaction signing.
 *
 * The EIP-712 standard allows users to sign typed, structured data instead of raw bytes.
 * This provides better UX as wallets can show users exactly what they're signing.
 *
 * Example usage:
 * ```kotlin
 * val typedDataJson = Eip712TypedDataBuilder.build(
 *     gaslessTransaction = gaslessTransactionData,
 *     chainId = 1,
 *     verifyingContract = "0x1234..."
 * )
 * val signature = wallet.signTypedData(typedDataJson)
 * ```
 */
object Eip712TypedDataBuilder {

    private const val DOMAIN_NAME = "Tangem7702GaslessExecutor"
    private const val DOMAIN_VERSION = "1"
    private const val PRIMARY_TYPE = "GaslessTransaction"
    private const val PRIMARY_TYPE_BATCH = "GaslessBatchTransaction"

    /**
     * Builds EIP-712 typed data JSON for gasless transaction.
     *
     * @param gaslessTransaction domain model with transaction and fee data
     * @param chainId blockchain network chain ID
     * @param verifyingContract address of the deployed gasless executor contract
     * @return JSON string ready for EIP-712 signing
     */
    fun build(
        gaslessTransaction: GaslessTransactionData,
        chainId: Int,
        verifyingContract: String,
        includeGasLimit: Boolean = true,
    ): String {
        val typedData = JSONObject().apply {
            put("types", buildTypes(includeGasLimit))
            put("primaryType", PRIMARY_TYPE)
            put("domain", buildDomain(chainId, verifyingContract))
            put("message", buildMessage(gaslessTransaction, includeGasLimit))
        }
        return typedData.toString()
    }

    /**
     * Builds EIP-712 typed data JSON for gasless batch transaction.
     *
     * @param gaslessBatch domain model with ordered list of transactions and fee data
     * @param chainId blockchain network chain ID
     * @param verifyingContract address of the deployed gasless executor contract
     * @return JSON string ready for EIP-712 signing
     */
    fun buildBatch(
        gaslessBatch: GaslessBatchTransactionData,
        chainId: Int,
        verifyingContract: String,
        includeGasLimit: Boolean = true,
    ): String {
        require(
            gaslessBatch.transactions.isNotEmpty(),
        ) { "GaslessBatchTransaction must contain at least one transaction" }
        val typedData = JSONObject().apply {
            put("types", buildBatchTypes(includeGasLimit))
            put("primaryType", PRIMARY_TYPE_BATCH)
            put("domain", buildDomain(chainId, verifyingContract))
            put("message", buildBatchMessage(gaslessBatch, includeGasLimit))
        }
        return typedData.toString()
    }

    /**
     * Builds the type definitions for all structures in the batch variant.
     * Uses `Transaction[]` for the ordered transactions array.
     */
    private fun buildBatchTypes(includeGasLimit: Boolean): JSONObject {
        return JSONObject().apply {
            put("EIP712Domain", buildEip712DomainTypeProperties())
            put("Transaction", buildTransactionTypeProperties(includeGasLimit))
            put("Fee", buildFeeTypeProperties())
            put("GaslessBatchTransaction", buildGaslessBatchTransactionTypeProperties())
        }
    }

    private fun buildGaslessBatchTransactionTypeProperties(): JSONArray {
        return JSONArray().apply {
            put(typeProperty("transactions", "Transaction[]"))
            put(typeProperty("fee", "Fee"))
            put(typeProperty("nonce", "uint256"))
        }
    }

    /**
     * Builds the message data from gasless batch transaction.
     */
    private fun buildBatchMessage(gaslessBatch: GaslessBatchTransactionData, includeGasLimit: Boolean): JSONObject {
        return JSONObject().apply {
            put("transactions", buildTransactionsArray(gaslessBatch.transactions, includeGasLimit))
            put("fee", buildFeeMessage(gaslessBatch.fee))
            put("nonce", gaslessBatch.nonce.toString())
        }
    }

    private fun buildTransactionsArray(
        transactions: List<GaslessTransactionData.Transaction>,
        includeGasLimit: Boolean,
    ): JSONArray {
        return JSONArray().apply {
            transactions.forEach { tx -> put(buildTransactionMessage(tx, includeGasLimit)) }
        }
    }

    /**
     * Builds the type definitions for all structures.
     * This schema is fixed and defines the structure of the data being signed.
     */
    private fun buildTypes(includeGasLimit: Boolean): JSONObject {
        return JSONObject().apply {
            put("EIP712Domain", buildEip712DomainTypeProperties())
            put("Transaction", buildTransactionTypeProperties(includeGasLimit))
            put("Fee", buildFeeTypeProperties())
            put("GaslessTransaction", buildGaslessTransactionTypeProperties())
        }
    }

    private fun buildGaslessTransactionTypeProperties(): JSONArray {
        return JSONArray().apply {
            put(typeProperty("transaction", "Transaction"))
            put(typeProperty("fee", "Fee"))
            put(typeProperty("nonce", "uint256"))
        }
    }

    /**
     * Creates a type property JSON object.
     */
    private fun typeProperty(name: String, type: String): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("type", type)
        }
    }

    /**
     * Builds the domain separator.
     */
    private fun buildDomain(chainId: Int, verifyingContract: String): JSONObject {
        return JSONObject().apply {
            put("name", DOMAIN_NAME)
            put("version", DOMAIN_VERSION)
            put("chainId", chainId)
            put("verifyingContract", verifyingContract)
        }
    }

    /**
     * Builds the message data from gasless transaction.
     */
    private fun buildMessage(gaslessTransaction: GaslessTransactionData, includeGasLimit: Boolean): JSONObject {
        return JSONObject().apply {
            put("transaction", buildTransactionMessage(gaslessTransaction.transaction, includeGasLimit))
            put("fee", buildFeeMessage(gaslessTransaction.fee))
            put("nonce", gaslessTransaction.nonce.toString())
        }
    }

    private fun buildTransactionMessage(
        transaction: GaslessTransactionData.Transaction,
        includeGasLimit: Boolean,
    ): JSONObject {
        return JSONObject().apply {
            put("to", transaction.to)
            put("value", transaction.value.toString())
            if (includeGasLimit) put("gasLimit", transaction.gasLimit.toString())
            put("data", transaction.data.toHexString())
        }
    }

    // region Shared type schema helpers

    private fun buildEip712DomainTypeProperties(): JSONArray {
        return JSONArray().apply {
            put(typeProperty("name", "string"))
            put(typeProperty("version", "string"))
            put(typeProperty("chainId", "uint256"))
            put(typeProperty("verifyingContract", "address"))
        }
    }

    private fun buildTransactionTypeProperties(includeGasLimit: Boolean): JSONArray {
        return JSONArray().apply {
            put(typeProperty("to", "address"))
            put(typeProperty("value", "uint256"))
            if (includeGasLimit) put(typeProperty("gasLimit", "uint256"))
            put(typeProperty("data", "bytes"))
        }
    }

    private fun buildFeeTypeProperties(): JSONArray {
        return JSONArray().apply {
            put(typeProperty("feeToken", "address"))
            put(typeProperty("maxTokenFee", "uint256"))
            put(typeProperty("coinPriceInToken", "uint256"))
            put(typeProperty("feeTransferGasLimit", "uint256"))
            put(typeProperty("baseGas", "uint256"))
            put(typeProperty("feeReceiver", "address"))
        }
    }

    // endregion

    // region Shared message helpers

    private fun buildFeeMessage(fee: GaslessTransactionData.Fee): JSONObject {
        return JSONObject().apply {
            put("feeToken", fee.feeToken)
            put("maxTokenFee", fee.maxTokenFee.toString())
            put("coinPriceInToken", fee.coinPriceInToken.toString())
            put("feeTransferGasLimit", fee.feeTransferGasLimit.toString())
            put("baseGas", fee.baseGas.toString())
            put("feeReceiver", fee.feeReceiver)
        }
    }

    // endregion
}