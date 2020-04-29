package com.tangem.blockchain.network.blockchair

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlockchairAddress(
        @Json(name = "data")
        val data: Map<String, BlockchairAddressData>? = null
)

data class BlockchairAddressData(
        @Json(name = "address")
        val addressInfo: BlockchairAddressInfo? = null,

        @Json(name = "utxo")
        val unspentOutputs: List<BlockchairUnspentOutput>? = null,

        @Json(name = "transactions")
        val transactions: List<String>? = null
)

data class BlockchairAddressInfo(
        @Json(name = "balance")
        val balance: Long? = null,

        @Json(name = "script_hex")
        val script: String? = null,

        @Json(name = "output_count")
        val outputCount: Int? = null,

        @Json(name = "unspent_output_count")
        val unspentOutputCount: Int? = null
)

data class BlockchairUnspentOutput(
        @Json(name = "block_id")
        val block: Int? = null,

        @Json(name = "transaction_hash")
        val transactionHash: String? = null,

        @Json(name = "index")
        val index: Int? = null,

        @Json(name = "value")
        val amount: Long? = null
)

data class BlockchairTransaction(
        @Json(name = "data")
        val data: Map<String, BlockchairTransactionData>? = null
)

data class BlockchairTransactionData(
        @Json(name = "transaction")
        val transaction: BlockchairTransactionInfo? = null
)

data class BlockchairTransactionInfo(
        @Json(name = "block_id")
        val block: Int? = null
)

data class BlockchairStats(
        @Json(name = "data")
        val data: BlockchairStatsData? = null
)

data class BlockchairStatsData(
        @Json(name = "suggested_transaction_fee_per_byte_sat")
        val feePerByte: Int? = null
)