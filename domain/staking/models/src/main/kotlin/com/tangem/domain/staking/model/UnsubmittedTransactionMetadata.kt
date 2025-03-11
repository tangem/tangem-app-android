package com.tangem.domain.staking.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UnsubmittedTransactionMetadata(
    @Json(name = "transactionHash")
    val transactionHash: String,
    @Json(name = "transactionId")
    val transactionId: String,
)