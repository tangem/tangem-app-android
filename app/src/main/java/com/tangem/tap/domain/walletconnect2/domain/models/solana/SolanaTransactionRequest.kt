package com.tangem.tap.domain.walletconnect2.domain.models.solana

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.tap.domain.walletconnect2.domain.WcRequestData

@JsonClass(generateAdapter = true)
data class SolanaTransactionRequest(
    @Json(name = "feePayer")
    val feePayer: String?,

    @Json(name = "transaction")
    val transaction: String,
) : WcRequestData