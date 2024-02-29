package com.tangem.tap.domain.userWalletList

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.domain.wallets.legacy.UserWalletsListManagerFeatureToggles

internal class DefaultUserWalletsListManagerFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : UserWalletsListManagerFeatureToggles {

    override val isGeneralManagerEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "GENERAL_USER_WALLETS_LIST_MANAGER_ENABLED")
}