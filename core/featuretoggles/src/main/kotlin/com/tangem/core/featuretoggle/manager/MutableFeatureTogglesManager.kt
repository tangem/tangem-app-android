package com.tangem.core.featuretoggle.manager

/**
 * Component for change information about the availability of feature toggles
 *
 * @author Andrew Khokhlov on 26/01/2023
 */
interface MutableFeatureTogglesManager : FeatureTogglesManager {

    /** Get feature toggles */
    fun getFeatureToggles(): Map<String, Boolean>

    /** Change availability [isEnabled] of toggle with name [name] */
    fun changeToggle(name: String, isEnabled: Boolean)
}
