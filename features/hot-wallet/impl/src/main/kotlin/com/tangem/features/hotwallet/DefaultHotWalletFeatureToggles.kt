package com.tangem.features.hotwallet

import android.content.Context
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.google.GoogleServicesHelper

internal class DefaultHotWalletFeatureToggles(
    private val context: Context,
    private val featureTogglesManager: FeatureTogglesManager,
) : HotWalletFeatureToggles {

    override val isGoogleDriveBackupEnabled: Boolean
        get() = GoogleServicesHelper.checkGoogleServicesAvailability(context)

    override val isMultipleHotWalletsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1799_MULTIPLE_HOT_WALLETS_ENABLED)
}