package com.tangem.feature.wallet.featuretoggles

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import javax.inject.Inject

internal class DefaultWalletFeatureToggles @Inject constructor(
    private val featureToggles: FeatureTogglesManager,
) : WalletFeatureToggles {

    override val isWalletReorderFeatureEnabled: Boolean
        get() = featureToggles.isFeatureEnabled("WALLET_REORDER_FEATURE_ENABLED")

    override val isMainScreenQrScanningEnabled: Boolean
        get() = featureToggles.isFeatureEnabled("MAIN_SCREEN_QR_SCANNING_ENABLED")
}