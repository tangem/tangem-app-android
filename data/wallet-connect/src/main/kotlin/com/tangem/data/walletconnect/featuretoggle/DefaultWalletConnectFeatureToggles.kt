package com.tangem.data.walletconnect.featuretoggle

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.walletconnect.featuretoggle.WalletConnectFeatureToggles

internal class DefaultWalletConnectFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : WalletConnectFeatureToggles {

    override val isBitcoinEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.WALLET_CONNECT_BITCOIN_ENABLED)

    override val isOnDemandInitEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18021_WALLETCONNECT_ON_DEMAND_INIT_ENABLED)

    override val isInterruptedApprovalRecoveryEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            FeatureToggles.AND_18136_WALLETCONNECT_INTERRUPTED_APPROVAL_RECOVERY_ENABLED,
        )
}