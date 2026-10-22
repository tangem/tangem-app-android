package com.tangem.features.hotwallet

import android.content.Context
import com.tangem.google.GoogleServicesHelper

internal class DefaultHotWalletFeatureToggles(
    private val context: Context,
) : HotWalletFeatureToggles {

    override val isGoogleDriveBackupEnabled: Boolean
        get() = GoogleServicesHelper.checkGoogleServicesAvailability(context)
}