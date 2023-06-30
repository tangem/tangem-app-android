package com.tangem.tap.features.details.redux

import com.tangem.blockchain.common.Wallet
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.entities.FiatCurrency
import org.rekotlin.Action

sealed class DetailsAction : Action {

    data class PrepareScreen(
        val scanResponse: ScanResponse,
        val wallets: List<Wallet>,
    ) : DetailsAction()

    object ReCreateTwinsWallet : DetailsAction()

    sealed class ResetToFactory : DetailsAction() {
        object Start : ResetToFactory()
        object Proceed : ResetToFactory()
        data class Confirm(val confirmed: Boolean) : ResetToFactory()
        object Failure : ResetToFactory()
        object Success : ResetToFactory()
    }

    object ScanCard : DetailsAction()

    data class PrepareCardSettingsData(val card: CardDTO) : DetailsAction()
    object ResetCardSettingsData : DetailsAction()

    sealed class AccessCodeRecovery : DetailsAction() {
        object Open : AccessCodeRecovery()
        data class SaveChanges(val enabled: Boolean) : AccessCodeRecovery() {
            data class Success(val enabled: Boolean) : AccessCodeRecovery()
        }

        data class SelectOption(val enabled: Boolean) : AccessCodeRecovery()
    }

    sealed class ManageSecurity : DetailsAction() {
        object OpenSecurity : ManageSecurity()
        data class SelectOption(val option: SecurityOption) : ManageSecurity()
        object SaveChanges : ManageSecurity() {
            object Success : ManageSecurity()
            object Failure : ManageSecurity()
        }

        object ChangeAccessCode : ManageSecurity()
    }

    sealed class AppSettings : DetailsAction() {
        data class SwitchPrivacySetting(
            val enable: Boolean,
            val setting: AppSetting,
        ) : AppSettings() {
            object Success : AppSettings()

            data class Failure(
                val prevState: Boolean,
                val setting: AppSetting,
            ) : AppSettings()
        }

        data class CheckBiometricsStatus(
            val awaitStatusChange: Boolean,
        ) : AppSettings()

        object EnrollBiometrics : AppSettings()
        data class BiometricsStatusChanged(
            val needEnrollBiometrics: Boolean,
        ) : AppSettings()
    }

    data class ChangeAppCurrency(val fiatCurrency: FiatCurrency) : DetailsAction()
}