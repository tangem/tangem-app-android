package com.tangem.datasource.api.auth.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Client-reported device metadata, included in both [AuthenticationPayload] and [RegisterPayload].
 * Mirrors the `DeviceMetadata` schema in the backend OpenAPI contract.
 */
@JsonClass(generateAdapter = true)
data class DeviceMetadata(
    /** Device hardware model (e.g. `iPhone 15 Pro`). */
    @Json(name = "deviceModel") val deviceModel: String?,
    /** Operating system (`android` / `ios`). */
    @Json(name = "os") val os: String,
    /** OS version string (e.g. `17.4.1`). */
    @Json(name = "osVersion") val osVersion: String?,
    /** Application version (e.g. `5.8.0`). */
    @Json(name = "appVersion") val appVersion: String?,
    /** User-Agent header (e.g. `Tangem/5.8.0 (iPhone; iOS 17.4.1; Scale/3.00)`). */
    @Json(name = "userAgent") val userAgent: String?,
    /** Client locale (e.g. `en-US`). */
    @Json(name = "locale") val locale: String?,
    /** Client timezone (e.g. `Europe/Moscow`). */
    @Json(name = "timezone") val timezone: String?,
)