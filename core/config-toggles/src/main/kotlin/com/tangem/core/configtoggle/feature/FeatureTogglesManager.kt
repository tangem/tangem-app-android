package com.tangem.core.configtoggle.feature

import com.tangem.core.configtoggle.FeatureToggles

/**
 * Component for getting information about the availability of feature toggles
 *
[REDACTED_AUTHOR]
 */
interface FeatureTogglesManager {

    /** Check feature [toggle] availability */
    fun isFeatureEnabled(toggle: FeatureToggles): Boolean
}