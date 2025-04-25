package com.tangem.tap.domain.walletconnect2.toggles

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class WalletConnectFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) {

    val isRedesignedWalletConnectEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("WALLET_CONNECT_REDESIGN_ENABLED")
}