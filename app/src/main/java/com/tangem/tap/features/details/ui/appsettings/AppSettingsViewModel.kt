package com.tangem.tap.features.details.ui.appsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.AppSetting
import com.tangem.tap.features.details.redux.AppSettingsState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.rekotlin.Store

internal class AppSettingsViewModel(private val store: Store<AppState>) {

    private val itemsFactory = AppSettingsItemsFactory()
    private val alertsFactory = AppSettingsAlertsFactory()

    var uiState: AppSettingsScreenState by mutableStateOf(AppSettingsScreenState.Loading)
        private set

    fun updateState(state: DetailsState) {
        uiState = AppSettingsScreenState.Content(
            items = buildItems(state.appSettingsState),
            alert = (uiState as? AppSettingsScreenState.Content)?.alert,
        )
    }

    fun checkBiometricsStatus() {
        store.dispatch(DetailsAction.AppSettings.CheckBiometricsStatus(awaitStatusChange = false))
    }

    fun refreshBiometricsStatus() {
        store.dispatch(DetailsAction.AppSettings.CheckBiometricsStatus(awaitStatusChange = true))
    }

    private fun buildItems(state: AppSettingsState): ImmutableList<AppSettingsScreenState.Item> {
        val items = buildList {
            if (state.needEnrollBiometrics) {
                itemsFactory.createEnrollBiometricsCard(onClick = ::enrollBiometrics).let(::add)
            }

            if (state.isBiometricsAvailable) {
                val canUseBiometrics = !state.needEnrollBiometrics && !state.isInProgress

                itemsFactory.createSaveWalletsSwitch(
                    isChecked = state.saveWallets,
                    isEnabled = canUseBiometrics,
                    onCheckedChange = ::onSaveWalletsToggled,
                ).let(::add)

                itemsFactory.createSaveAccessCodeSwitch(
                    isChecked = state.saveAccessCodes,
                    isEnabled = canUseBiometrics,
                    onCheckedChange = ::onSaveAccessCodesToggled,
                ).let(::add)
            }
        }

        return items.toImmutableList()
    }

    private fun enrollBiometrics() {
        store.dispatchOnMain(DetailsAction.AppSettings.EnrollBiometrics)
    }

    private fun onSaveWalletsToggled(isChecked: Boolean) {
        if (isChecked) {
            onSettingsToggled(AppSetting.SaveWallets, enable = true)
        } else {
            updateContentState {
                copy(
                    alert = alertsFactory.createDeleteSavedWalletsAlert(
                        onDelete = {
                            onSettingsToggled(AppSetting.SaveWallets, enable = false)
                            dismissDialog()
                        },
                        onDismiss = ::dismissDialog,
                    ),
                )
            }
        }
    }

    private fun onSaveAccessCodesToggled(isChecked: Boolean) {
        if (isChecked) {
            onSettingsToggled(AppSetting.SaveAccessCode, enable = true)
        } else {
            updateContentState {
                copy(
                    alert = alertsFactory.createDeleteSavedAccessCodesAlert(
                        onDelete = {
                            onSettingsToggled(AppSetting.SaveAccessCode, enable = false)
                            dismissDialog()
                        },
                        onDismiss = ::dismissDialog,
                    ),
                )
            }
        }
    }

    private fun onSettingsToggled(setting: AppSetting, enable: Boolean) {
        store.dispatch(DetailsAction.AppSettings.SwitchPrivacySetting(enable = enable, setting = setting))
    }

    private fun dismissDialog() {
        updateContentState { copy(alert = null) }
    }

    private fun updateContentState(block: AppSettingsScreenState.Content.() -> AppSettingsScreenState.Content) {
        uiState = when (val state = uiState) {
            is AppSettingsScreenState.Content -> block(state)
            is AppSettingsScreenState.Loading -> state
        }
    }
}