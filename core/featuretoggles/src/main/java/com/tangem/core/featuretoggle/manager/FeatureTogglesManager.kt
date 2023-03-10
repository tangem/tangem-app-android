package com.tangem.core.featuretoggle.manager

import com.tangem.core.featuretoggle.FeatureToggle

/**
 * Component for getting information about the availability of feature toggles
 *
* [REDACTED_AUTHOR]
 */
interface FeatureTogglesManager {

    /** Initialize manager */
    suspend fun init()

    /** Check feature toggle [toggle] availability */
    fun isFeatureEnabled(toggle: FeatureToggle): Boolean
}
