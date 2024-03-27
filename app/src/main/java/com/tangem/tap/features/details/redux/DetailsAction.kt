package com.tangem.tap.features.details.redux

import androidx.lifecycle.LifecycleCoroutineScope
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.Action

sealed class DetailsAction : Action {

    data class PrepareScreen(
        val scanResponse: ScanResponse,
        val shouldSaveUserWallets: Boolean,
    ) : DetailsAction()

    data object ReCreateTwinsWallet : DetailsAction()

    sealed class ResetToFactory : DetailsAction() {
        data object Start : ResetToFactory()
        data object Proceed : ResetToFactory()
        data class AcceptCondition1(val accepted: Boolean) : ResetToFactory()
        data class AcceptCondition2(val accepted: Boolean) : ResetToFactory()
        data object Failure : ResetToFactory()
        data object Success : ResetToFactory()

        data class LastWarningDialogVisibility(val isShown: Boolean) : ResetToFactory()
    }

    data object ScanCard : DetailsAction()

    data class PrepareCardSettingsData(val card: CardDTO, val cardTypesResolver: CardTypesResolver) : DetailsAction()
    data object ResetCardSettingsData : DetailsAction()
    data object ScanAndSaveUserWallet : DetailsAction() {

        data object Success : DetailsAction()

        data class Error(val error: TextReference?) : DetailsAction()
    }

    data object DismissError : DetailsAction()

    sealed class AccessCodeRecovery : DetailsAction() {
        object Open : AccessCodeRecovery()
        data class SaveChanges(val enabled: Boolean) : AccessCodeRecovery() {
            data class Success(val enabled: Boolean) : AccessCodeRecovery()
        }

        data class SelectOption(val enabled: Boolean) : AccessCodeRecovery()
    }

    sealed class ManageSecurity : DetailsAction() {
        data object OpenSecurity : ManageSecurity()
        data class SelectOption(val option: SecurityOption) : ManageSecurity()
        data object SaveChanges : ManageSecurity() {
            data object Success : ManageSecurity()
            data object Failure : ManageSecurity()
        }

        data object ChangeAccessCode : ManageSecurity()
    }

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
    }

    data class ChangeAppCurrency(val currency: AppCurrency) : DetailsAction()
}
