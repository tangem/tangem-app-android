package com.tangem.tap.features.details.redux

import androidx.lifecycle.LifecycleCoroutineScope
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.Action

sealed class DetailsAction : Action {

    data class PrepareScreen(
        val scanResponse: ScanResponse,
        val shouldSaveUserWallets: Boolean,
    ) : DetailsAction()

    data object ScanAndSaveUserWallet : DetailsAction() {

        data object Success : DetailsAction()

        data class Error(val error: TextReference?) : DetailsAction()
    }

    data object DismissError : DetailsAction()

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
            val lifecycleScope: LifecycleCoroutineScope,
        ) : AppSettings()

        data object EnrollBiometrics : AppSettings()
        data class BiometricsStatusChanged(
            val needEnrollBiometrics: Boolean,
        ) : AppSettings()

        data class ChangeAppThemeMode(
            val appThemeMode: AppThemeMode,
        ) : AppSettings()

        data class ChangeBalanceHiding(
            val hideBalance: Boolean,
        ) : AppSettings()

        data class ChangeAppCurrency(
            val currency: AppCurrency,
        ) : AppSettings()

        data class Prepare(val state: AppSettingsState) : AppSettings()
    }

    data class ChangeAppCurrency(val currency: AppCurrency) : DetailsAction()
}