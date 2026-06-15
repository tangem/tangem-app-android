package com.tangem.core.configtoggle.feature

import com.tangem.core.configtoggle.FeatureToggles

/** Version value marking a feature toggle that has no planned release (permanently disabled). */
const val DISABLED_FEATURE_TOGGLE_VERSION = "undefined"

/**
 * Component for getting information about the availability of feature toggles
 *
[REDACTED_AUTHOR]
 */
interface FeatureTogglesManager {

    /** Check feature [toggle] availability */
    fun isFeatureEnabled(toggle: FeatureToggles): Boolean
}