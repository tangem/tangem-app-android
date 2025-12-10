package com.tangem.data.walletconnect.featuretoggle

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.walletconnect.featuretoggle.WalletConnectFeatureToggles

internal class DefaultWalletConnectFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : WalletConnectFeatureToggles {

    override val isBitcoinEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "WALLET_CONNECT_BITCOIN_ENABLED")
}