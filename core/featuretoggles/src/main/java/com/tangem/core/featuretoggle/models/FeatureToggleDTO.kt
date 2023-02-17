package com.tangem.core.featuretoggle.models

/**
 * Data model with information about feature toggle
 *
 * @property name    feature toggle name
 * @property version version number in which the feature will be included
 *
 * IMPORTANT: if the version is "undefined", it means that feature toggle is disabled!
 *
 * @author Andrew Khokhlov on 25/01/2023
 */
internal data class FeatureToggleDTO(val name: String, val version: String)
