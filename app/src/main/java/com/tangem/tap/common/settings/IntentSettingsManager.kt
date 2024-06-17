package com.tangem.tap.common.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.tap.foregroundActivityObserver
import com.tangem.tap.withForegroundActivity

internal class IntentSettingsManager : SettingsManager {
    override fun openSettings() {
        foregroundActivityObserver.withForegroundActivity { activity ->
            val openSettingsIntent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.packageName, null),
            )
            activity.startActivity(openSettingsIntent)
        }
    }
}
