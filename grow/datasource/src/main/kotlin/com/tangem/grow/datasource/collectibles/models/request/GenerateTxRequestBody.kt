package com.tangem.grow.datasource.collectibles.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Body for POST /api/v1/collectible/generateTx
 *
 * @property toAddress the pack is opened to this address instead of [fromAddress]
 */
@JsonClass(generateAdapter = true)
data class GenerateTxRequestBody(
    @Json(name = "fromAddress")
    val fromAddress: String,
    @Json(name = "collectibleType")
    val collectibleType: String,
    @Json(name = "toAddress")
    val toAddress: String,
    @Json(name = "turboMode")
    val turboMode: TurboModeDTO? = null,
) {

    @JsonClass(generateAdapter = true)
    data class TurboModeDTO(
        @Json(name = "enabled")
        val isEnabled: Boolean = false,
        @Json(name = "buybackAddress")
        val buybackAddress: String? = null,
    )
}