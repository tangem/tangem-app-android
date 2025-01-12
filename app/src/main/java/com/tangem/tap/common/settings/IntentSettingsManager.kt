package com.tangem.tap.common.settings

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.hardware.biometrics.BiometricManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.tangem.core.navigation.settings.SettingsManager

internal class IntentSettingsManager(val context: Context) : SettingsManager {

    override fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )

        open(intent = intent)
    }

    override fun openBiometricSettings() {
        val settingsAction = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Settings.ACTION_BIOMETRIC_ENROLL
            else -> Settings.ACTION_SECURITY_SETTINGS
        }

        val intent = Intent(settingsAction).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG,
                )
            }
        }

        open(intent = intent)
    }

    private fun open(intent: Intent) {
        context.startActivity(
            intent.apply {
                flags = FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
        )
    }
}