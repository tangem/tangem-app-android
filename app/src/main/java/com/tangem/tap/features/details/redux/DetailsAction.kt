package com.tangem.tap.features.details.redux

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.apptheme.model.AppThemeMode
import kotlinx.coroutines.CoroutineScope
import org.rekotlin.Action

@Suppress("BooleanPropertyNaming")
sealed class DetailsAction : Action {

    sealed class AppSettings : DetailsAction() {
        data class SwitchPrivacySetting(
            val enable: Boolean,
            val setting: AppSetting,
        ) : AppSettings() {
            data object Success : AppSettings()

            data class Failure(
                val prevState: Boolean,
                val setting: AppSetting,
            ) : AppSettings()
        }

        data class CheckBiometricsStatus(
            val coroutineScope: CoroutineScope,
        ) : AppSettings()

        data object EnrollBiometrics : AppSettings()
        data class BiometricsStatusChanged(
            val isEnrollBiometricsNeeded: Boolean,
        ) : AppSettings()

        data class ChangeAppThemeMode(
            val appThemeMode: AppThemeMode,
        ) : AppSettings()

        data class ChangeBalanceHiding(
            val shouldHideBalance: Boolean,
        ) : AppSettings()

        data class ChangeAppCurrency(
            val currency: AppCurrency,
        ) : AppSettings()

        data class Prepare(val state: AppSettingsState) : AppSettings()
    }
}