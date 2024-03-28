package com.tangem.tap.domain.walletconnect2.toggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class WalletConnectFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) {

    val isSolanaTxSignEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "WC_SOLANA_TX_SIGN_ENABLED")
}
