package com.tangem.tap.features.details.ui.appsettings

import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.AppSetting
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import org.rekotlin.Store

class AppSettingsViewModel(private val store: Store<AppState>) {

    fun updateState(state: DetailsState): AppSettingsScreenState {
        return with(state.appSettingsState) {
            AppSettingsScreenState(
                settings = mapOf(
                    AppSetting.SaveWallets to saveWallets,
                    AppSetting.SaveAccessCode to saveAccessCodes,
                ),
                showEnrollBiometricsCard = needEnrollBiometrics,
                isTogglesEnabled = !needEnrollBiometrics && !isInProgress,
                onSettingToggled = { privacySetting, enabled ->
                    onSettingsToggled(privacySetting, enabled)
                },
                onEnrollBiometrics = {
                    store.dispatchOnMain(DetailsAction.AppSettings.EnrollBiometrics)
                },
            )
        }
    }

    private fun onSettingsToggled(setting: AppSetting, enable: Boolean) {
        store.dispatch(DetailsAction.AppSettings.SwitchPrivacySetting(enable = enable, setting = setting))
    }

    fun checkBiometricsStatus() {
        store.dispatch(DetailsAction.AppSettings.CheckBiometricsStatus(awaitStatusChange = false))
    }

    fun refreshBiometricsStatus() {
        store.dispatch(DetailsAction.AppSettings.CheckBiometricsStatus(awaitStatusChange = true))
    }
}
