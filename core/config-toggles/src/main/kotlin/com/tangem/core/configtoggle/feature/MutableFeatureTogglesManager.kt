package com.tangem.core.configtoggle.feature

/**
 * Component for change information about the availability of feature toggles
 *
[REDACTED_AUTHOR]
 */
interface MutableFeatureTogglesManager : FeatureTogglesManager {

    /** Check if the current state of the feature toggles matches the local config state. */
    fun isMatchLocalConfig(): Boolean

    /** Get feature toggles */
    fun getFeatureToggles(): Map<String, Boolean>

    /** Change availability [isEnabled] of toggle with name [name] */
    suspend fun changeToggle(name: String, isEnabled: Boolean)

    /** Recover local config state */
    suspend fun recoverLocalConfig()
}