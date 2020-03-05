package com.tangem.blockchain.bitcoin.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlockcypherResponse(
        @Json(name = "address")
        val address: String? = null,

        @Json(name = "balance")
        val balance: Long? = null,

        @Json(name = "unconfirmed_balance")
        val unconfirmedBalance: Long? = null,

        @Json(name = "txrefs")
        val txrefs: List<BlockcypherTxref>? = null
)

@JsonClass(generateAdapter = true)
data class BlockcypherTxref(
        @Json(name = "tx_hash")
        val hash: String? = null,

        @Json(name = "tx_output_n")
        val outputIndex: Int? = null,

        @Json(name = "value")
        val amount: Long? = null,

        @Json(name = "confirmations")
        val confirmations: Long? = null,

        @Json(name = "script")
        val outputScript: String? = null
)

@JsonClass(generateAdapter = true)
data class BlockcypherTx(
        @Json(name = "hex")
        val hex: String? = null
)

@JsonClass(generateAdapter = true)
data class BlockcypherFee(
        @Json(name = "low_fee_per_kb")
        val minFeePerKb: Long? = null,

        @Json(name = "medium_fee_per_kb")
        val normalFeePerKb: Long? = null,

        @Json(name = "high_fee_per_kb")
        val priorityFeePerKb: Long? = null
)