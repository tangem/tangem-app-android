package com.tangem.datasource.api.stakekit.models.request

import com.squareup.moshi.Json

data class YieldBalanceRequestBody(
    @Json(name = "addresses") val addresses: Address,
    @Json(name = "args") val args: YieldBalanceRequestArgs,
) {

    data class YieldBalanceRequestArgs(
        @Json(name = "validatorAddresses") val validatorAddresses: List<String>,
    )
}