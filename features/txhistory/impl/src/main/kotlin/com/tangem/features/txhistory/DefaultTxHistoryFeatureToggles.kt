package com.tangem.features.txhistory

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultTxHistoryFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : TxHistoryFeatureToggles {
    override val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("TX_HISTORY_REFACTORING_ENABLED")
}