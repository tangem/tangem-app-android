package com.tangem.data.network.model

import com.google.gson.annotations.SerializedName

data class TokenEmvTransferBody(
        val contract: String,
        val amount: String,
        val recipient: String,
        @SerializedName("fee_limit")
        val feeLimit: String,
        val sequence: Int,
        val r: String,
        val s: String,
        val v: Int
)