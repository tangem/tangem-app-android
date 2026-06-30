package com.tangem.feature.wallet.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import javax.inject.Inject

internal class DefaultWalletFeatureToggles @Inject constructor(
    private val featureToggles: FeatureTogglesManager,
) : WalletFeatureToggles {

    override val isAddFundsStage1Enabled: Boolean
        get() = featureToggles.isFeatureEnabled(FeatureToggles.AND_15310_ADD_FUNDS_STAGE1)

    override val isManageFundsEnabled: Boolean
        get() = featureToggles.isFeatureEnabled(FeatureToggles.TWI_1377_MANAGE_FUNDS)
}