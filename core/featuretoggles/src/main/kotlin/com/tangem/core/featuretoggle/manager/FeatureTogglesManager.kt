package com.tangem.core.featuretoggle.manager

/**
 * Component for getting information about the availability of feature toggles
 *
[REDACTED_AUTHOR]
 */
interface FeatureTogglesManager {

    /** Initialize manager */
    suspend fun init()

    /** Check feature toggle availability by name [name] */
    fun isFeatureEnabled(name: String): Boolean
}