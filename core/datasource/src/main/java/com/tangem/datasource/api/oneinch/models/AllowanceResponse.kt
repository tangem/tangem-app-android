package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

data class AllowanceResponse(
    @Json(name = "allowance") val allowance: String,
)
