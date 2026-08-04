package com.tangem.features.hotwallet

import android.content.Context
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.google.GoogleServicesHelper

internal class DefaultHotWalletFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
    private val context: Context,
) : HotWalletFeatureToggles {

    override val isGoogleDriveBackupEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_922_GOOGLE_DRIVE_BACKUP_ENABLED) &&
            GoogleServicesHelper.checkGoogleServicesAvailability(context)
}