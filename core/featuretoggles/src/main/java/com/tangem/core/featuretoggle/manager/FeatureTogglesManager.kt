package com.tangem.core.featuretoggle.manager

/**
 * Component for getting information about the availability of feature toggles
 *
 * @author Andrew Khokhlov on 26/01/2023
 */
interface FeatureTogglesManager {

    /** Initialize manager */
    suspend fun init()

    /** Check feature toggle [toggle] availability */
    fun isFeatureEnabled(toggle: IFeatureToggle): Boolean
}
