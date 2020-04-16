package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class TokenEmvTransferBody(
        val CID: String,
        val publicKey: String,
        val amount: String,
        val currency: String,
        val recipient: String,
        @SerializedName("fee_limit")
        val feeLimit: String,
        val sequence: Int,
        val signature: String
)

data class TokenEmvTransferAnswer(
        val error: String?,
        val errorCode: Int?,
        val success: Boolean?,
        val tx_id: String?,
        val blockchain_tx_id: String?
)

data class TokenEmvGetTransferFeeBody(
        val CID: String,
        val publicKey: String
)

data class TokenEmvGetTransferFeeAnswer(
        val error: String?,
        val errorCode: Int?,
        val success: Boolean?,
        val fee: String?,
        val currency: String?
)