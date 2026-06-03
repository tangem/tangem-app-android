package com.tangem.core.configtoggle.feature

/**
 * Feature toggle information exposed by [MutableFeatureTogglesManager].
 *
 * @property name      raw toggle name
 * @property version   release version from local config ("undefined" for permanently disabled toggles)
 * @property isEnabled current toggle state (may differ from default if overridden locally)
 */
data class FeatureToggleInfo(
    val name: String,
    val version: String,
    val isEnabled: Boolean,
)