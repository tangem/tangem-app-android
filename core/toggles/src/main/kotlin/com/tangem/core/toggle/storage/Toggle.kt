package com.tangem.core.toggle.storage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data model with information about config toggle
 *
 * @property name    toggle name
 * @property version version number in which the feature will be included
 *
 * IMPORTANT: if the version is "undefined", it means that toggle is disabled!
 *
 * @author Andrew Khokhlov on 25/01/2023
 */
@JsonClass(generateAdapter = true)
internal data class Toggle(
    @Json(name = "name") val name: String,
    @Json(name = "version") val version: String,
)
