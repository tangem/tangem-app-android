package com.tangem.tap.domain.walletconnect2.domain.models.solana

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.tap.domain.walletconnect2.domain.WcRequestData

@JsonClass(generateAdapter = true)
data class SolanaTransactionRequest(
    @Json(name = "feePayer")
    val feePayer: String,

    @Json(name = "recentBlockhash")
    val recentBlockhash: String,

    @Json(name = "instructions")
    val instructions: List<Instruction>,
) : WcRequestData {

    @JsonClass(generateAdapter = true)
    data class Instruction(
        @Json(name = "programId")
        val programId: String,

        @Json(name = "data")
        val data: List<Byte>,

        @Json(name = "keys")
        val keys: List<Key>,
    )

    @JsonClass(generateAdapter = true)
    data class Key(
        @Json(name = "isSigner")
        val isSigner: Boolean,

        @Json(name = "isWritable")
        val isWritable: Boolean,

        @Json(name = "pubkey")
        val publicKey: String,
    )
}