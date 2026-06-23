package com.tangem.data.staking.verification.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class TronStakingRawTx(
    @Json(name = "raw_data") val rawData: RawData?,
) {

    @JsonClass(generateAdapter = true)
    internal data class RawData(
        @Json(name = "contract") val contract: List<Contract>?,
    )

    @JsonClass(generateAdapter = true)
    internal data class Contract(
        @Json(name = "type") val type: String?,
    )
}