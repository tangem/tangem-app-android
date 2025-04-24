package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationApplicationUpdateBody(
    @Json(name = "pushToken") val pushToken: String,
    @Json(name = "systemVersion") val systemVersion: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
)