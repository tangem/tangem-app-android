package com.tangem.domain.walletmanager.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SmartContractMethod(
    @Json(name = "info") val info: String?,
    @Json(name = "source") val source: String?,
    @Json(name = "name") val name: String,
)