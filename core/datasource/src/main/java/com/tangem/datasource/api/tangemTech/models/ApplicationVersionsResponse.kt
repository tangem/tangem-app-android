package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `GET v1/application/versions`. All fields are nullable — the backend may omit any, in
 * which case the corresponding check is skipped.
 *
 * @property minSupportedVersion   app version threshold for a mandatory update: if
 *   `installedVersion <= minSupportedVersion` the app must force-update (or show "update your OS"
 *   when [minSupportedOSVersion] is not met). Inclusive. E.g. "5.30".
 * @property minSupportedOSVersion minimal device OS version required to install the update for the
 *   [minSupportedVersion] case: if `deviceOsVersion < minSupportedOSVersion` the device can't update
 *   and the "OS too old" screen is shown. Exclusive.
 * @property criticalVersion       app version threshold for a critical mandatory update: if
 *   `installedVersion <= criticalVersion` the app must force-update (or permanently "brick" when
 *   [criticalOSVersion] is not met). Inclusive.
 * @property criticalOSVersion     minimal device OS version required to install the critical update:
 *   if `deviceOsVersion < criticalOSVersion` the app is bricked (update impossible). Exclusive.
 * @property latestVersion         latest available app version: if `installedVersion < latestVersion`
 *   an optional update is offered. Exclusive. E.g. "5.40".
 */
@JsonClass(generateAdapter = true)
data class ApplicationVersionsResponse(
    @Json(name = "minSupportedVersion") val minSupportedVersion: String?,
    @Json(name = "minSupportedOSVersion") val minSupportedOSVersion: String?,
    @Json(name = "criticalVersion") val criticalVersion: String?,
    @Json(name = "criticalOSVersion") val criticalOSVersion: String?,
    @Json(name = "latestVersion") val latestVersion: String?,
)