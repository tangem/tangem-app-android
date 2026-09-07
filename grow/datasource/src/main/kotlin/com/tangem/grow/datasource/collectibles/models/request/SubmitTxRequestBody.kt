package com.tangem.grow.datasource.collectibles.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Body for POST /api/v1/collectible/submitTx */
@JsonClass(generateAdapter = true)
data class SubmitTxRequestBody(
    @Json(name = "signedTxData")
    val signedTxData: String,
)