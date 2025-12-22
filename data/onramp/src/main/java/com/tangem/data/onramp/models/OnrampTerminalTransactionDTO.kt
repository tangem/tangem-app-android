package com.tangem.data.onramp.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Model for terminal onramp transaction
 *
 * @property txId           Transaction ID
 * @property terminatedAt   Termination timestamp in milliseconds
 */
@JsonClass(generateAdapter = true)
data class OnrampTerminalTransactionDTO(
    @Json(name = "txId")
    val txId: String,
    @Json(name = "terminatedAt")
    val terminatedAt: Long,
)