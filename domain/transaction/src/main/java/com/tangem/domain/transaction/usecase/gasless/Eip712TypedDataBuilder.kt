package com.tangem.domain.transaction.usecase.gasless

import com.tangem.common.extensions.toHexString
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

    /**
     * Builds EIP-712 typed data JSON for gasless transaction.
     *
     * @param gaslessTransaction domain model with transaction and fee data
     * @param chainId blockchain network chain ID
     * @param verifyingContract address of the deployed gasless executor contract
     * @return JSON string ready for EIP-712 signing
     */
    fun build(gaslessTransaction: GaslessTransactionData, chainId: Int, verifyingContract: String): String {
        val typedData = JSONObject().apply {
            put("types", buildTypes())
            put("primaryType", PRIMARY_TYPE)
            put("domain", buildDomain(chainId, verifyingContract))
            put("message", buildMessage(gaslessTransaction))
        }
        return typedData.toString()
    }

    /**
     * Builds the type definitions for all structures.
     * This schema is fixed and defines the structure of the data being signed.
     */
    @Suppress("NestedScopeFunctions")
    private fun buildTypes(): JSONObject {
        return JSONObject().apply {
            put("EIP712Domain", JSONArray().apply {
                put(typeProperty("name", "string"))
                put(typeProperty("version", "string"))
                put(typeProperty("chainId", "uint256"))
                put(typeProperty("verifyingContract", "address"))
            })
            put("Transaction", JSONArray().apply {
                put(typeProperty("to", "address"))
                put(typeProperty("value", "uint256"))
                put(typeProperty("data", "bytes"))
            })
            put("Fee", JSONArray().apply {
                put(typeProperty("feeToken", "address"))
                put(typeProperty("maxTokenFee", "uint256"))
                put(typeProperty("coinPriceInToken", "uint256"))
                put(typeProperty("feeTransferGasLimit", "uint256"))
                put(typeProperty("baseGas", "uint256"))
            })
            put("GaslessTransaction", JSONArray().apply {
                put(typeProperty("transaction", "Transaction"))
                put(typeProperty("fee", "Fee"))
                put(typeProperty("nonce", "uint256"))
            })
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
    @Suppress("NestedScopeFunctions")
    private fun buildMessage(gaslessTransaction: GaslessTransactionData): JSONObject {
        return JSONObject().apply {
            put("transaction", JSONObject().apply {
                put("to", gaslessTransaction.transaction.to)
                put("value", gaslessTransaction.transaction.value.toString())
                put("data", gaslessTransaction.transaction.data.toHexString())
            })
            put("fee", JSONObject().apply {
                put("feeToken", gaslessTransaction.fee.feeToken)
                put("maxTokenFee", gaslessTransaction.fee.maxTokenFee.toString())
                put("coinPriceInToken", gaslessTransaction.fee.coinPriceInToken.toString())
                put("feeTransferGasLimit", gaslessTransaction.fee.feeTransferGasLimit.toString())
                put("baseGas", gaslessTransaction.fee.baseGas.toString())
            })
            put("nonce", gaslessTransaction.nonce.toString())
        }
    }
}