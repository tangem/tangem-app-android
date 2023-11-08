package com.tangem.datasource.config.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

sealed interface ChatConfig

@JsonClass(generateAdapter = true)
data class SprinklrConfig(
    @Json(name = "appID") val appId: String,
    @Json(name = "apiKey") val apiKey: String,
    @Json(name = "environment") val environment: String,
) : ChatConfig