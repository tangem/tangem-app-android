package com.tangem.core.configtoggle.feature.provider

/**
 * Provider for feature toggle entries.
 * This interface allows for easier testing by enabling mock implementations.
 */
internal interface FeatureTogglesProvider {

    /**
     * Returns a map of toggle names to their version strings.
     * The version string is "undefined" for disabled toggles, or a version number like "1.0.0" for enabled ones.
     */
    fun getToggles(): Map<String, String>
}