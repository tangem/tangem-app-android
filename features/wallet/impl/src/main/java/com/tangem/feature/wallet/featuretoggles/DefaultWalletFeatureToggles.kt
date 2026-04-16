package com.tangem.feature.wallet.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import javax.inject.Inject

internal class DefaultWalletFeatureToggles @Inject constructor(
    private val featureToggles: FeatureTogglesManager,
) : WalletFeatureToggles {

    override val isAddAndManageTokensEnabled: Boolean
        get() = featureToggles.isFeatureEnabled(FeatureToggles.ADD_AND_MANAGE_TOKENS_ENABLED)
}