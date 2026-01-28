package com.tangem.features.send.v2

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.send.v2.api.SendFeatureToggles
import javax.inject.Inject

internal class DefaultSendFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : SendFeatureToggles {
    override val isGaslessTransactionsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("GASLESS_TRANSACTIONS_ENABLED")
}