package com.tangem.tap.common.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.tangem.core.navigation.settings.SettingsManager

internal class IntentSettingsManager(val context: Context) : SettingsManager {
    override fun openSettings() {
        val openSettingsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
        context.startActivity(openSettingsIntent)
    }
}