package com.tangem.core.configtoggle.storage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data model with information about config toggle
 *
 * @property name    toggle name
 * @property version version in which the toggle will be enabled
 *
 * IMPORTANT: if the version is "undefined", it means that toggle is disabled!
 *
[REDACTED_AUTHOR]
 */
@JsonClass(generateAdapter = true)
internal data class ConfigToggle(
    @Json(name = "name") val name: String,
    @Json(name = "version") val version: String,
)