package com.tangem.datasource.api.stakekit.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YieldBalanceRequestBody(
    @Json(name = "addresses") val addresses: Address,
    @Json(name = "args") val args: YieldBalanceRequestArgs,
    @Json(name = "integrationId") val integrationId: String,
) {

    @JsonClass(generateAdapter = true)
    data class YieldBalanceRequestArgs(
        @Json(name = "validatorAddresses") val validatorAddresses: List<String>,
    )
}