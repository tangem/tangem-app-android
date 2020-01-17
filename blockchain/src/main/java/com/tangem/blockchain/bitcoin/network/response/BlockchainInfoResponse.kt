package com.tangem.blockchain.bitcoin.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlockchainInfoAddress(
        @Json(name = "final_balance")
        val finalBalance: Long? = null,

        @Json(name = "txs")
        val transactions: List<BlockchainInfoTransaction>? = null
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoTransaction(
        @Json(name = "hash")
        val hash: String? = null,

        @Json(name = "block_height")
        val blockHeight: Long? = null
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoUnspents(
        @Json(name = "unspent_outputs")
        val unspentOutputs: List<BlockchainInfoUtxo>
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoUtxo(
        @Json(name = "tx_hash_big_endian")
        val hash: String? = null,

        @Json(name = "tx_output_n")
        val outputIndex: Int? = null,

        @Json(name = "value")
        val amount: Long? = null,

        @Json(name = "script")
        val outputScript: String? = null
)