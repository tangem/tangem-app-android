package com.tangem.feature.wallet.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.wallet.WalletFeatureToggles
import javax.inject.Inject

internal class DefaultWalletFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : WalletFeatureToggles {

    override val isRedesignedAccountRowsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18663_REDESIGNED_ACCOUNT_ROWS_ENABLED)

    override val isWalletLinkingEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_19037_WALLET_LINKING_ENABLED)
}