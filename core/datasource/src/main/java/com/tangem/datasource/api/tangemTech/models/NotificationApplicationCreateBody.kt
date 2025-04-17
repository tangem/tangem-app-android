package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationApplicationCreateBody(
    @Json(name = "pushToken") val pushToken: String,
    @Json(name = "platform") val platform: String,
    @Json(name = "device") val device: String,
    @Json(name = "systemVersion") val systemVersion: String,
    @Json(name = "language") val language: String,
    @Json(name = "timezone") val timezone: String,
)
