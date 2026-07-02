package com.tangem.domain.appupdate.model

/**
 * Backend-driven update policy. All fields are nullable; a null threshold skips its check.
 *
 * @property minSupportedVersion   mandatory-update threshold (inclusive): `installedVersion <= minSupportedVersion`
 * @property minSupportedOSVersion OS threshold for the min-supported case (exclusive): `deviceOsVersion < it` -> OS too old
 * @property criticalVersion       critical-update threshold (inclusive): `installedVersion <= criticalVersion`
 * @property criticalOSVersion     OS threshold for the critical case (exclusive): `deviceOsVersion < it` -> brick
 * @property latestVersion         optional-update threshold (exclusive): `installedVersion < latestVersion`
 */
data class AppVersionInfo(
    val minSupportedVersion: String?,
    val minSupportedOSVersion: String?,
    val criticalVersion: String?,
    val criticalOSVersion: String?,
    val latestVersion: String?,
)