package com.tangem.grow.datasource.collectibles.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response for POST /api/v1/collectible/generateTx
 *
 * @property txData base64-encoded unsigned transaction
 */
@JsonClass(generateAdapter = true)
data class GenerateTxResponse(
    @Json(name = "id")
    val id: String,
    @Json(name = "txData")
    val txData: String,
)