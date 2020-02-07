package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class BitcoreBalance(
        @SerializedName("confirmed")
        var confirmed: Long? = null,

        @SerializedName("unconfirmed")
        var unconfirmed: Long? = null
)

data class BitcoreUtxo(
        @SerializedName("mintTxid")
        var mintTxid: String? = null,

        @SerializedName("mintIndex")
        var mintIndex: Int? = null,

        @SerializedName("value")
        var value: Long? = null,

        @SerializedName("script")
        var script: String? = null
)

data class BitcoreBalanceAndUnspents(
        var balance: BitcoreBalance,
        var unspents: List<BitcoreUtxo>
)

data class BitcoreSendResponse(
        var txid: String? = null
)