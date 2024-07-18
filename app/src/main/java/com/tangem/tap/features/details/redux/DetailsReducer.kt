package com.tangem.tap.features.details.redux

import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.models.scan.CardDTO
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
            handlePrepareScreen(action, state)
        }
        is DetailsAction.AppSettings -> {
            handlePrivacyAction(action, detailsState)
        }
        is DetailsAction.ChangeAppCurrency -> detailsState.copy(
            appSettingsState = detailsState.appSettingsState.copy(
                selectedAppCurrency = action.currency,
            ),
        )
        is DetailsAction.ScanAndSaveUserWallet -> detailsState.copy(
            isScanningInProgress = true,
        )
        is DetailsAction.ScanAndSaveUserWallet.Error -> detailsState.copy(
            isScanningInProgress = false,
            error = action.error,
        )
        is DetailsAction.ScanAndSaveUserWallet.Success -> detailsState.copy(
            isScanningInProgress = false,
        )
        is DetailsAction.DismissError -> detailsState.copy(
            error = null,
        )
    }
}

private fun handlePrepareScreen(action: DetailsAction.PrepareScreen, state: AppState): DetailsState {
    return DetailsState(
        scanResponse = action.scanResponse,
        createBackupAllowed = action.scanResponse.card.backupStatus == CardDTO.BackupStatus.NoBackup,
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
        is DetailsAction.AppSettings.Prepare -> state.copy(
            appSettingsState = action.state,
        )
        is DetailsAction.AppSettings.EnrollBiometrics,
        is DetailsAction.AppSettings.CheckBiometricsStatus,
        -> state
    }
}
