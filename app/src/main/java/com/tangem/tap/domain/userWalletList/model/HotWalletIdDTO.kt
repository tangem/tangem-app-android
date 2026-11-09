package com.tangem.tap.domain.userWalletList.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.hot.sdk.model.HotWalletId

// Keys must match HotWalletId, stored by reflection before
@JsonClass(generateAdapter = true)
internal data class HotWalletIdDTO(
    @Json(name = "value")
    val value: String,
    @Json(name = "authType")
    val authType: HotWalletId.AuthType,
)