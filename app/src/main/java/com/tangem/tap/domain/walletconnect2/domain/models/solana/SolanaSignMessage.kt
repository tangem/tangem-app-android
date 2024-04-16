package com.tangem.tap.domain.walletconnect2.domain.models.solana

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SolanaSignMessage(
    @Json(name = "pubkey")
    val publicKey: String,

    @Json(name = "message")
    val message: String,
)