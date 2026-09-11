package com.tangem.data.jointaccount.safe

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The `eth_signTypedData_v4` payload of a Safe transaction, as the schema of Safe v1.5.0 defines it.
 *
 * The field order is the schema, not formatting: EIP-712 encodes `SafeTx` in the order its type declares, so
 * moving a line here changes every digest. The type declarations themselves never vary and are the defaults
 * below; only the domain and the message come from the transaction.
 */
@Serializable
internal data class SafeTxTypedData(
    val types: Types = Types(),
    val primaryType: String = PRIMARY_TYPE,
    val domain: Domain,
    val message: Message,
) {

    @Serializable
    data class Types(
        @SerialName("EIP712Domain")
        val eip712Domain: List<Field> = listOf(
            Field(name = "chainId", type = UINT256),
            Field(name = "verifyingContract", type = ADDRESS),
        ),
        @SerialName(PRIMARY_TYPE)
        val safeTx: List<Field> = listOf(
            Field(name = "to", type = ADDRESS),
            Field(name = "value", type = UINT256),
            Field(name = "data", type = "bytes"),
            Field(name = "operation", type = "uint8"),
            Field(name = "safeTxGas", type = UINT256),
            Field(name = "baseGas", type = UINT256),
            Field(name = "gasPrice", type = UINT256),
            Field(name = "gasToken", type = ADDRESS),
            Field(name = "refundReceiver", type = ADDRESS),
            Field(name = "nonce", type = UINT256),
        ),
    )

    @Serializable
    data class Field(val name: String, val type: String)

    @Serializable
    data class Domain(val chainId: Long, val verifyingContract: String)

    /**
     * The `uint256` fields are strings and `operation` is a number, the way `eth_signTypedData_v4` callers state
     * them: a `uint256` beyond `Long` has no JSON number to be stated as.
     */
    @Serializable
    data class Message(
        val to: String,
        val value: String,
        val data: String,
        val operation: Int,
        val safeTxGas: String,
        val baseGas: String,
        val gasPrice: String,
        val gasToken: String,
        val refundReceiver: String,
        val nonce: String,
    )

    companion object {
        const val PRIMARY_TYPE = "SafeTx"
        private const val UINT256 = "uint256"
        private const val ADDRESS = "address"
    }
}