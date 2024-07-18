package com.tangem.tap.features.details.redux

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.StateType

data class DetailsState(
    val scanResponse: ScanResponse? = null,
    val privacyPolicyUrl: String? = null,
    val createBackupAllowed: Boolean = false,
    val isScanningInProgress: Boolean = false,
    val error: TextReference? = null,
    val appSettingsState: AppSettingsState = AppSettingsState(),
) : StateType

sealed class ResetCardDialog {
    data object StartResetDialog : ResetCardDialog()
    data object ContinueResetDialog : ResetCardDialog()
    data object InterruptedResetDialog : ResetCardDialog()
    data object CompletedResetDialog : ResetCardDialog()
}

data class AppSettingsState(
    val saveWallets: Boolean = false,
    val saveAccessCodes: Boolean = false,
    val isBiometricsAvailable: Boolean = false,
    val needEnrollBiometrics: Boolean = false,
    val isHidingEnabled: Boolean = false,
    val isInProgress: Boolean = false,
    val selectedAppCurrency: AppCurrency = AppCurrency.Default,
    val selectedThemeMode: AppThemeMode = AppThemeMode.DEFAULT,
)

enum class SecurityOption { LongTap, PassCode, AccessCode }

enum class AppSetting {
    SaveWallets, SaveAccessCode
}