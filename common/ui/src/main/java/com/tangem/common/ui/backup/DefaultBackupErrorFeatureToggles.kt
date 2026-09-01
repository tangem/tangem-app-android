package com.tangem.common.ui.backup

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

class DefaultBackupErrorFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : BackupErrorFeatureToggles {

    override val isTopUpWarningEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1741_TOP_UP_WARNING_ENABLED)
}