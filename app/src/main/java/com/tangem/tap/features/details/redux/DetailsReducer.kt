package com.tangem.tap.features.details.redux

import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.rekotlin.Action

object DetailsReducer {
    fun reduce(action: Action, state: AppState): DetailsState = internalReduce(action, state)
}

@Suppress("CyclomaticComplexMethod")
private fun internalReduce(action: Action, state: AppState): DetailsState {
    if (action !is DetailsAction) return state.detailsState
    val detailsState = state.detailsState
    return when (action) {
        is DetailsAction.PrepareScreen -> {
            handlePrepareScreen(action)
        }
        is DetailsAction.AppSettings -> {
            handlePrivacyAction(action, detailsState)
        }
        is DetailsAction.ChangeAppCurrency -> detailsState.copy(
            appSettingsState = detailsState.appSettingsState.copy(
                selectedAppCurrency = action.currency,
            ),
        )
    }
}

private fun handlePrepareScreen(action: DetailsAction.PrepareScreen): DetailsState {
    return DetailsState(
        scanResponse = action.scanResponse,
        appSettingsState = AppSettingsState(
            isBiometricsAvailable = runBlocking {
                tangemSdkManager.checkCanUseBiometry()
            },
            saveWallets = action.shouldSaveUserWallets,
            saveAccessCodes = runBlocking {
                store.inject(DaggerGraphState::settingsRepository).shouldSaveAccessCodes()
            },
            selectedAppCurrency = store.state.globalState.appCurrency,
            selectedThemeMode = runBlocking {
                store.inject(DaggerGraphState::appThemeModeRepository).getAppThemeMode().firstOrNull()
                    ?: AppThemeMode.DEFAULT
            },
            isHidingEnabled = runBlocking {
                store.inject(DaggerGraphState::balanceHidingRepository)
                    .getBalanceHidingSettings().isHidingEnabledInSettings
            },
            needEnrollBiometrics = runBlocking {
                runCatching(tangemSdkManager::needEnrollBiometrics).getOrNull() ?: false
            },
        ),
    )
}

private fun handlePrivacyAction(action: DetailsAction.AppSettings, state: DetailsState): DetailsState {
    return when (action) {
        is DetailsAction.AppSettings.SwitchPrivacySetting -> state.copy(
            appSettingsState = when (action.setting) {
                AppSetting.SaveWallets -> state.appSettingsState.copy(
                    isInProgress = true,
                    saveWallets = action.enable,
                )
                AppSetting.SaveAccessCode -> state.appSettingsState.copy(
                    isInProgress = true,
                    saveWallets = true, // User can't enable access codes saving without wallets saving
                    saveAccessCodes = action.enable,
                )
            },
        )
        is DetailsAction.AppSettings.SwitchPrivacySetting.Success -> state.copy(
            appSettingsState = state.appSettingsState.copy(
                isInProgress = false,
            ),
        )
        is DetailsAction.AppSettings.SwitchPrivacySetting.Failure -> state.copy(
            appSettingsState = when (action.setting) {
                AppSetting.SaveWallets -> state.appSettingsState.copy(
                    isInProgress = false,
                    saveWallets = action.prevState,
                )
                AppSetting.SaveAccessCode -> state.appSettingsState.copy(
                    isInProgress = false,
                    saveAccessCodes = action.prevState,
                )
            },
        )
        is DetailsAction.AppSettings.BiometricsStatusChanged -> state.copy(
            appSettingsState = state.appSettingsState.copy(
                needEnrollBiometrics = action.needEnrollBiometrics,
            ),
        )
        is DetailsAction.AppSettings.ChangeAppThemeMode -> state.copy(
            appSettingsState = state.appSettingsState.copy(
                selectedThemeMode = action.appThemeMode,
            ),
        )
        is DetailsAction.AppSettings.ChangeAppCurrency -> state.copy(
            appSettingsState = state.appSettingsState.copy(
                selectedAppCurrency = action.currency,
            ),
        )
        is DetailsAction.AppSettings.ChangeBalanceHiding -> state.copy(
            appSettingsState = state.appSettingsState.copy(
                isHidingEnabled = action.hideBalance,
            ),
        )
        // state should be copied to avoid concurrent modifications from different sources
        is DetailsAction.AppSettings.Prepare -> state.copy(
            appSettingsState = state.appSettingsState.copy(
                saveWallets = action.state.saveWallets,
                saveAccessCodes = action.state.saveAccessCodes,
                isBiometricsAvailable = action.state.isBiometricsAvailable,
                isHidingEnabled = action.state.isHidingEnabled,
                selectedAppCurrency = action.state.selectedAppCurrency,
                selectedThemeMode = action.state.selectedThemeMode,
            ),
        )
        is DetailsAction.AppSettings.EnrollBiometrics,
        is DetailsAction.AppSettings.CheckBiometricsStatus,
        -> state
    }
}