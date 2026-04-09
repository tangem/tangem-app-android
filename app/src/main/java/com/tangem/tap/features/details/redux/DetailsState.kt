package com.tangem.tap.features.details.redux

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.apptheme.model.AppThemeMode
import org.rekotlin.StateType

data class DetailsState(
    val appSettingsState: AppSettingsState = AppSettingsState(),
) : StateType

@Suppress("BooleanPropertyNaming")
data class AppSettingsState(
    val requireAccessCode: Boolean = false,
    val useBiometricAuthentication: Boolean = false,
    val needEnrollBiometrics: Boolean = false,
    val hasSecuredWallets: Boolean = false,
    val isHidingEnabled: Boolean = false,
    val isInProgress: Boolean = false,
    val selectedAppCurrency: AppCurrency = AppCurrency.Default,
    val selectedThemeMode: AppThemeMode = AppThemeMode.DEFAULT,
)

enum class SecurityOption { LongTap, PassCode, AccessCode }

enum class AppSetting {
    RequireAccessCode, BiometricAuthentication,
}