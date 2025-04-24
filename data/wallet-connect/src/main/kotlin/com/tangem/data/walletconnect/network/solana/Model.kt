package com.tangem.data.walletconnect.network.solana

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class WcSolanaSignMessageRequest(
    @Json(name = "pubkey")
    val publicKey: String,

    @Json(name = "message")
    val message: String,
)

@JsonClass(generateAdapter = true)
internal data class WcSolanaSignTransactionRequest(
    @Json(name = "transaction")
    val transaction: String,
)