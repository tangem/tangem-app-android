package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class BlockchairAddressResponse(
        @SerializedName("data")
        val data: Map<String, BlockchairAddressData>? = null
)

data class BlockchairAddressData(
        @SerializedName("address")
        val address: BlockchairAddressInfo? = null,

        @SerializedName("utxo")
        val unspentOutputs: List<BlockchairUnspentOutput>? = null,

        @SerializedName("transactions")
        val transactions: List<String>
)

data class BlockchairAddressInfo(
        @SerializedName("balance")
        val balance: Long? = null,

        @SerializedName("output_count")
        val outputCount: Int? = null,

        @SerializedName("unspent_output_count")
        val unspentOutputCount: Int? = null
)

data class BlockchairUnspentOutput(
        @SerializedName("block_id")
        val block: Int? = null,

        @SerializedName("transaction_hash")
        val transactionHash: String? = null,

        @SerializedName("index")
        val index: Int? = null,

        @SerializedName("value")
        val amount: Long? = null
)

data class BlockchairTransactionResponse(
        @SerializedName("data")
        val data: Map<String, BlockchairTransactionData>? = null
)

data class BlockchairTransactionData(
        @SerializedName("transaction")
        val transaction: BlockchairTransactionInfo? = null
)

data class BlockchairTransactionInfo(
        @SerializedName("block_id")
        val block: Int? = null
)

data class BlockchairStatsResponse(
        @SerializedName("data")
        val data: BlockchairStatsData? = null
)

data class BlockchairStatsData(
        @SerializedName("suggested_transaction_fee_per_byte_sat")
        val feePerByte: Int? = null
)