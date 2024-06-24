package com.tangem.tap

import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher

interface ActivityResultCaller {
    val activityResultLauncher: ActivityResultLauncher<Intent>?
}

internal fun ActivityResultCaller.openSystemBiometrySettings() {
    val settingsAction = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            Settings.ACTION_BIOMETRIC_ENROLL
        }
        else -> {
            Settings.ACTION_SECURITY_SETTINGS
        }
    }
    val intent = Intent(settingsAction).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BiometricManager.Authenticators.BIOMETRIC_STRONG,
            )
        }
    }

    activityResultLauncher?.launch(intent)
}