package com.tangem.features.txhistory.di

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import javax.inject.Inject

internal class DefaultTxHistoryFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : TxHistoryFeatureToggles {

    override val isSolanaTxHistoryEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.SOLANA_TX_HISTORY_ENABLED)

    override val isNewTxHistoryEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_15767_NEW_TX_HISTORY_ENABLED)
}