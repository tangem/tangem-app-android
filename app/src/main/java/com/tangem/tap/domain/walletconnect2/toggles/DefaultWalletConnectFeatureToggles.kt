package com.tangem.tap.domain.walletconnect2.toggles

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.walletconnect.components.WalletConnectFeatureToggles

internal class DefaultWalletConnectFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : WalletConnectFeatureToggles {

    override val isRedesignedWalletConnectEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("WALLET_CONNECT_REDESIGN_ENABLED")
}