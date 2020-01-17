package com.tangem.blockchain.bitcoin.network.response

import com.squareup.moshi.Json

data class BlockcypherResponse(
        @Json(name = "address")
        var address: String? = null,

        @Json(name = "balance")
        var balance: Long? = null,

        @Json(name = "unconfirmed_balance")
        var unconfirmedBalance: Long? = null,

        @Json(name = "txrefs")
        var txrefs: List<BlockcypherTxref>? = null
)

data class BlockcypherTxref(
        @Json(name = "tx_hash")
        var hash: String? = null,

        @Json(name = "tx_output_n")
        var outputIndex: Int? = null,

        @Json(name = "value")
        var amount: Long? = null,

        @Json(name = "confirmations")
        var confirmations: Long? = null,

        @Json(name = "script")
        var outputScript: String? = null
)

data class BlockcypherTx(
        @Json(name = "hex")
        var hex: String? = null
)

data class BlockcypherFee(
        @Json(name = "low_fee_per_kb")
        var minFeePerKb: Long? = null,

        @Json(name = "medium_fee_per_kb")
        var normalFeePerKb: Long? = null,

        @Json(name = "high_fee_per_kb")
        var priorityFeePerKb: Long? = null
)

data class BlockcypherBody(val tx: String)