package com.tangem.tap.features.details.redux

import com.tangem.tap.common.redux.AppState
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
        appSettingsState = action.initializedAppSettingsState,
    )
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
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
                AppSetting.RequireAccessCode -> state.appSettingsState.copy(
                    isInProgress = true,
                    requireAccessCode = action.enable,
                )
                AppSetting.BiometricAuthentication -> state.appSettingsState.copy(
                    isInProgress = true,
                    useBiometricAuthentication = action.enable,
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
                AppSetting.RequireAccessCode -> state.appSettingsState.copy(
                    isInProgress = false,
                    requireAccessCode = action.prevState,
                )
                AppSetting.BiometricAuthentication -> state.appSettingsState.copy(
                    isInProgress = false,
                    needEnrollBiometrics = action.prevState,
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