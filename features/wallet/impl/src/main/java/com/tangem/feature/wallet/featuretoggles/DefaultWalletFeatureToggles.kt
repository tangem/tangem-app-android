package com.tangem.feature.wallet.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles

/**
 * Default implementation of CustomToken feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 *
 * @author Andrew Khokhlov on 04/04/2023
 */
internal class DefaultWalletFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : WalletFeatureToggles {

    override val isRedesignedScreenEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "REDESIGNED_WALLET_SCREEN_ENABLED")
}
