package com.tangem.core.featuretoggle.storage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data model with information about feature toggle
 *
 * @property name    feature toggle name
 * @property version version number in which the feature will be included
 *
 * IMPORTANT: if the version is "undefined", it means that feature toggle is disabled!
 *
[REDACTED_AUTHOR]
 */
@JsonClass(generateAdapter = true)
internal data class FeatureToggle(
    @Json(name = "name") val name: String,
    @Json(name = "version") val version: String,
)