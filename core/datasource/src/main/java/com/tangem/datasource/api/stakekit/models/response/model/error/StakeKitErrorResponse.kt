package com.tangem.datasource.api.stakekit.models.response.model.error

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class StakeKitErrorResponse(
    @Json(name = "message")
    val message: String? = null,
    @Json(name = "code")
    val code: Int? = null,
    @Json(name = "path")
    val path: String? = null,

    @Json(name = "details")
    val details: StakeKitErrorDetailsDTO? = null,
    @Json(name = "level")
    val level: String? = null, // unused

    // 403
    @Json(name = "type")
    val type: String? = null,
    @Json(name = "countryCode")
    val countryCode: String?,
    @Json(name = "regionCode")
    val regionCode: String? = null,
    @Json(name = "tags")
    val tags: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class StakeKitErrorDetailsDTO(
    @Json(name = "arguments")
    val arguments: String? = null,
    @Json(name = "amount")
    val amount: String? = null,
    @Json(name = "yieldId")
    val yieldId: String? = null,
)