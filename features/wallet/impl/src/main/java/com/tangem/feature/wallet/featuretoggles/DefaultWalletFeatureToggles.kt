package com.tangem.feature.wallet.featuretoggles

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import javax.inject.Inject

internal class DefaultWalletFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : WalletFeatureToggles {

    override val isMainActionButtonsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "MAIN_ACTION_BUTTONS_ENABLED")
}