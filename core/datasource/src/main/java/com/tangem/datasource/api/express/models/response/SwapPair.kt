package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.express.models.request.LeastTokenInfo

@JsonClass(generateAdapter = true)
data class SwapPair(
    @Json(name = "from")
    val from: LeastTokenInfo,

    @Json(name = "to")
    val to: LeastTokenInfo,

    @Json(name = "providers")
    val providers: List<SwapPairProvider>,

)

@JsonClass(generateAdapter = true)
data class SwapPairProvider(
    @Json(name = "providerId")
    val providerId: String,

    @Json(name = "rateTypes")
    val rateTypes: List<RateType>,
)

@JsonClass(generateAdapter = false)
enum class RateType {
    @Json(name = "float")
    FLOAT,

    @Json(name = "fixed")
    FIXED,
}