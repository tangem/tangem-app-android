package com.tangem.feature.wallet.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles

/**
 * Default implementation of Wallet feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 *
[REDACTED_AUTHOR]
 */
internal class DefaultWalletFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : WalletFeatureToggles {

    override val isWalletsScrollingPreviewEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "WALLETS_SCROLLING_PREVIEW_ENABLED")
}