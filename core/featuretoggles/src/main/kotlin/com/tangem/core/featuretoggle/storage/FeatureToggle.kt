package com.tangem.core.featuretoggle.storage

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
internal data class FeatureToggle(val name: String, val version: String)