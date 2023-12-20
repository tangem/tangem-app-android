package com.tangem.tap.features.details.ui.appsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleCoroutineScope
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.AppSetting
import com.tangem.tap.features.details.redux.AppSettingsState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.scope
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.rekotlin.Store

internal class AppSettingsViewModel(
    private val store: Store<AppState>,
    private val appCurrencyRepository: AppCurrencyRepository,
) {

    private val itemsFactory = AppSettingsItemsFactory()
    private val dialogsFactory = AppSettingsDialogsFactory()

    private val appCurrencyUpdatesJobHolder = JobHolder()

    var uiState: AppSettingsScreenState by mutableStateOf(AppSettingsScreenState.Loading)
        private set

    init {
        bootstrapAppCurrencyUpdates()
    }

    fun updateState(state: DetailsState) {
        uiState = AppSettingsScreenState.Content(
            items = buildItems(state.appSettingsState),
            dialog = (uiState as? AppSettingsScreenState.Content)?.dialog,
        )
    }

    fun checkBiometricsStatus(lifecycleScope: LifecycleCoroutineScope) {
        store.dispatch(DetailsAction.AppSettings.CheckBiometricsStatus(lifecycleScope))
    }

    private fun buildItems(state: AppSettingsState): ImmutableList<AppSettingsScreenState.Item> {
        val items = buildList {
            if (state.needEnrollBiometrics) {
                Analytics.send(Settings.AppSettings.EnableBiometrics)
                itemsFactory.createEnrollBiometricsCard(onClick = ::enrollBiometrics).let(::add)
            }

            itemsFactory.createSelectAppCurrencyButton(
                currentAppCurrencyName = state.selectedAppCurrency.name,
                onClick = ::showAppCurrencySelector,
            ).let(::add)

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

            itemsFactory.createFlipToHideBalanceSwitch(
                isChecked = state.isHidingEnabled,
                isEnabled = true,
                onCheckedChange = ::onFlipToHideBalanceToggled,
            ).let(::add)

            itemsFactory.createSelectThemeModeButton(state.selectedThemeMode) {
                showThemeModeSelector(state.selectedThemeMode)
            }.let(::add)
        }

        return items.toImmutableList()
    }

    private fun enrollBiometrics() {
        store.dispatchOnMain(DetailsAction.AppSettings.EnrollBiometrics)
    }

    private fun showAppCurrencySelector() {
        store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.AppCurrencySelector))
    }

    private fun showThemeModeSelector(selectedMode: AppThemeMode) {
        updateContentState {
            copy(
                dialog = dialogsFactory.createThemeModeSelectorDialog(
                    selectedModeIndex = selectedMode.ordinal,
                    onSelect = { mode ->
                        Analytics.send(
                            event = Settings.AppSettings.ThemeSwitched(
                                theme = AnalyticsParam.AppTheme.fromAppThemeMode(mode),
                            ),
                        )
                        store.dispatchOnMain(DetailsAction.AppSettings.ChangeAppThemeMode(mode))
                        dismissDialog()
                    },
                    onDismiss = ::dismissDialog,
                ),
            )
        }
    }

    private fun onSaveWalletsToggled(isChecked: Boolean) {
        if (isChecked) {
            onSettingsToggled(AppSetting.SaveWallets, enable = true)
        } else {
            updateContentState {
                copy(
                    dialog = dialogsFactory.createDeleteSavedWalletsAlert(
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
                    dialog = dialogsFactory.createDeleteSavedAccessCodesAlert(
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

    private fun onFlipToHideBalanceToggled(enable: Boolean) {
        val param = AnalyticsParam.OnOffState(enable)
        Analytics.send(Settings.AppSettings.HideBalanceChanged(param))

        store.dispatch(DetailsAction.AppSettings.ChangeBalanceHiding(hideBalance = enable))
    }

    private fun dismissDialog() {
        updateContentState { copy(dialog = null) }
    }

    private fun bootstrapAppCurrencyUpdates() {
        appCurrencyRepository
            .getSelectedAppCurrency()
            .onEach {
                if (it.code == store.state.globalState.appCurrency.code) return@onEach

                store.dispatchWithMain(DetailsAction.AppSettings.ChangeAppCurrency(it))
            }
            .launchIn(scope)
            .saveIn(appCurrencyUpdatesJobHolder)
    }

    private fun updateContentState(block: AppSettingsScreenState.Content.() -> AppSettingsScreenState.Content) {
        uiState = when (val state = uiState) {
            is AppSettingsScreenState.Content -> block(state)
            is AppSettingsScreenState.Loading -> state
        }
    }
}