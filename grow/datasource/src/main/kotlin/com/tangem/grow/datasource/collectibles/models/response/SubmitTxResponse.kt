package com.tangem.grow.datasource.collectibles.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response for POST /api/v1/collectible/submitTx */
@JsonClass(generateAdapter = true)
data class SubmitTxResponse(
    @Json(name = "id")
    val id: String,
)