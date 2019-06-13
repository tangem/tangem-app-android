package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class BlockcypherResponse(
        @SerializedName("address")
        var address: String? = null,

        @SerializedName("balance")
        var balance: Long? = null,

        @SerializedName("unconfirmed_balance")
        var unconfirmed_balance: Long? = null,

        @SerializedName("txrefs")
        var txrefs: List<BlockcypherTxref>? = null
)

data class BlockcypherTxref(
        @SerializedName("tx_hash")
        var tx_hash: String? = null,

        @SerializedName("tx_output_n")
        var tx_output_n: Int? = null,

        @SerializedName("value")
        var value: Long? = null,

        @SerializedName("confirmations")
        var confirmations: Long? = null,

        @SerializedName("script")
        var script: String? = null
)

data class BlockcypherFee(
        @SerializedName("low_fee_per_kb")
        var low_fee_per_kb: Long? = null,

        @SerializedName("medium_fee_per_kb")
        var medium_fee_per_kb: Long? = null,

        @SerializedName("high_fee_per_kb")
        var high_fee_per_kb: Long? = null
)