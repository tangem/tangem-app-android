package com.tangem.tap.features.details.redux

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.StateType

data class DetailsState(
    @Deprecated("Delete after onboarding refactoring")
    val scanResponse: ScanResponse? = null,
    val appSettingsState: AppSettingsState = AppSettingsState(),
) : StateType

@Suppress("BooleanPropertyNaming")
data class AppSettingsState(
    @Deprecated("Delete after hot wallet release")
    val saveWallets: Boolean = false,
    @Deprecated("Delete after hot wallet release")
    val saveAccessCodes: Boolean = false,
    @Deprecated("Delete after hot wallet release")
    val isBiometricsAvailable: Boolean = false,
    val requireAccessCode: Boolean = false,
    val useBiometricAuthentication: Boolean = false,
    val needEnrollBiometrics: Boolean = false,
    val isHidingEnabled: Boolean = false,
    val isInProgress: Boolean = false,
    val selectedAppCurrency: AppCurrency = AppCurrency.Default,
    val selectedThemeMode: AppThemeMode = AppThemeMode.DEFAULT,
)

enum class SecurityOption { LongTap, PassCode, AccessCode }

enum class AppSetting {
    SaveWallets, SaveAccessCode, RequireAccessCode, BiometricAuthentication,
}