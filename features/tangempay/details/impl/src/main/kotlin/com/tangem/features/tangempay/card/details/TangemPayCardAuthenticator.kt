package com.tangem.features.tangempay.card.details

import com.tangem.core.biometric.BiometricAuthError
import com.tangem.core.biometric.BiometricAuthManager
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

internal class TangemPayCardAuthenticator @Inject constructor(
    private val biometricAuthManager: BiometricAuthManager,
    private val uiMessageSender: UiMessageSender,
    private val settingsManager: SettingsManager,
) {

    suspend fun authenticate(title: TextReference): Boolean {
        val config = BiometricAuthManager.Config(title = title, subtitle = null)
        return when (val result = biometricAuthManager.authenticate(config)) {
            is BiometricAuthManager.Result.Success -> true
            is BiometricAuthManager.Result.Cancelled -> false
            is BiometricAuthManager.Result.Failure -> {
                showFailure(result.error)
                false
            }
        }
    }

    private fun showFailure(error: BiometricAuthError) {
        when (error) {
            BiometricAuthError.NoBiometricEnrolled,
            BiometricAuthError.NoDeviceCredential,
            -> {
                uiMessageSender.send(
                    message = TangemPayMessagesFactory.createProtectionNotSetMessage(
                        onOpenSettingsClick = settingsManager::openScreenLockSettings,
                    ),
                )
            }
            BiometricAuthError.HardwareUnavailable,
            BiometricAuthError.NoForegroundActivity,
            BiometricAuthError.Unknown,
            -> {
                uiMessageSender.send(
                    message = SnackbarMessage(resourceReference(CoreUiR.string.common_unknown_error)),
                )
            }
            BiometricAuthError.LockedOutPermanently -> {
                uiMessageSender.send(
                    message = SnackbarMessage(
                        resourceReference(CoreUiR.string.biometric_lockout_permanent_warning_title),
                    ),
                )
            }
            BiometricAuthError.SecurityUpdateRequired -> {
                uiMessageSender.send(
                    message = SnackbarMessage(
                        resourceReference(CoreUiR.string.alert_authentication_error_message),
                    ),
                )
            }
            BiometricAuthError.LockedOut,
            BiometricAuthError.SystemCancelled,
            BiometricAuthError.Timeout,
            -> Unit
        }
    }
}