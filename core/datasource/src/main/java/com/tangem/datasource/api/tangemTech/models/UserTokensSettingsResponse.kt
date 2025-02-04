package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserTokensSettingsResponse(
    @Json(name = "group")
    val group: UserTokensResponse.GroupType,
    @Json(name = "sort")
    val sort: UserTokensResponse.SortType,
)