package com.tangem.core.configtoggle.feature

/**
 * Component for getting information about the availability of feature toggles
 *
[REDACTED_AUTHOR]
 */
interface FeatureTogglesManager {

    /** Check feature toggle availability by name [name] */
    fun isFeatureEnabled(name: String): Boolean
}