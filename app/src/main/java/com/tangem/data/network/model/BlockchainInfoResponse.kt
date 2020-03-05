package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class BlockchainInfoAddress(
        @SerializedName("final_balance")
        var final_balance: Long? = null,

        @SerializedName("txs")
        var txs: List<BlockchainInfoTransaction>? = null
)

data class BlockchainInfoTransaction(
        @SerializedName("hash")
        var hash: String? = null,

        @SerializedName("block_height")
        var block_height: Long? = null,

        @SerializedName("inputs")
        var inputs: List<BlockchainInfoInput>
)

data class BlockchainInfoUnspents(
        @SerializedName("unspent_outputs")
        var unspent_outputs: List<BlockchainInfoUtxo>
)

data class BlockchainInfoUtxo(
        @SerializedName("tx_hash_big_endian")
        var tx_hash_big_endian: String? = null,

        @SerializedName("tx_output_n")
        var tx_output_n: Int? = null,

        @SerializedName("value")
        var value: Long? = null,

        @SerializedName("script")
        var script: String? = null
)

data class BlockchainInfoInput(
        @SerializedName("prev_out")
        var prev_out: BlockchainInfoOutput
)

data class BlockchainInfoOutput(
        @SerializedName("addr")
        var addr: String? = null
)

data class BlockchainInfoAddressAndUnspents(
        var address: BlockchainInfoAddress,
        var unspents: BlockchainInfoUnspents
)