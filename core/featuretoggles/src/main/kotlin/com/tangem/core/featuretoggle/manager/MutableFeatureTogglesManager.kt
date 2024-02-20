package com.tangem.core.featuretoggle.manager

/**
 * Component for change information about the availability of feature toggles
 *
[REDACTED_AUTHOR]
 */
interface MutableFeatureTogglesManager : FeatureTogglesManager {

    /** Get feature toggles */
    fun getFeatureToggles(): Map<String, Boolean>

    /** Change availability [isEnabled] of toggle with name [name] */
    suspend fun changeToggle(name: String, isEnabled: Boolean)
}