package com.tangem.datasource.api.stakekit.models.response.model.action

import com.squareup.moshi.Json

enum class StakingActionStatusDTO {
    @Json(name = "CANCELED")
    CANCELED,

    @Json(name = "CREATED")
    CREATED,

    @Json(name = "WAITING_FOR_NEXT")
    WAITING_FOR_NEXT,

    @Json(name = "PROCESSING")
    PROCESSING,

    @Json(name = "FAILED")
    FAILED,

    @Json(name = "SUCCESS")
    SUCCESS,

    UNKNOWN,
}