package com.tangem.tap.features.details.redux

import androidx.lifecycle.LifecycleCoroutineScope
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.utils.popTo
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber
import com.tangem.core.analytics.models.AnalyticsParam as CoreAnalyticsParam

class DetailsMiddleware {
    private val appSettingsMiddleware = AppSettingsMiddleware()
    val detailsMiddleware: Middleware<AppState> = { _, stateProvider ->
        { next ->
            { action ->
                if (!DemoHelper.tryHandle(stateProvider, action)) {
                    val detailsState = stateProvider()?.detailsState
                    if (detailsState != null) {
                        handleAction(detailsState, action)
                    }
                }
                next(action)
            }
        }
    }

    private fun handleAction(state: DetailsState, action: Action) {
        when (action) {
            is DetailsAction.AppSettings -> appSettingsMiddleware.handle(state, action)
            is DetailsAction.ScanAndSaveUserWallet -> scanAndSaveUserWallet()
        }
    }

    class AppSettingsMiddleware {

        private val checkBiometricsStatusJobHolder = JobHolder()

        fun handle(state: DetailsState, action: DetailsAction.AppSettings) {
            when (action) {
                is DetailsAction.AppSettings.SwitchPrivacySetting -> {
                    when (action.setting) {
                        AppSetting.SaveWallets -> toggleSaveWallets(state, enable = action.enable)
                        AppSetting.SaveAccessCode -> toggleSaveAccessCodes(state, enable = action.enable)
                    }
                }
                is DetailsAction.AppSettings.CheckBiometricsStatus -> {
                    observeBiometricsStatusChanges(action.lifecycleScope)
                }
                is DetailsAction.AppSettings.EnrollBiometrics -> {
                    enrollBiometrics()
                }
                is DetailsAction.AppSettings.ChangeAppThemeMode -> {
                    changeAppThemeMode(action.appThemeMode)
                }
                is DetailsAction.AppSettings.ChangeBalanceHiding -> {
                    changeBalanceHiding(action.hideBalance)
                }
                is DetailsAction.AppSettings.ChangeAppCurrency -> {
                    store.dispatch(GlobalAction.ChangeAppCurrency(action.currency))
                    store.dispatch(DetailsAction.ChangeAppCurrency(action.currency))
                }
                is DetailsAction.AppSettings.SwitchPrivacySetting.Success,
                is DetailsAction.AppSettings.SwitchPrivacySetting.Failure,
                is DetailsAction.AppSettings.BiometricsStatusChanged,
                is DetailsAction.AppSettings.Prepare,
                -> Unit
            }
        }

        private fun observeBiometricsStatusChanges(lifecycleScope: LifecycleCoroutineScope) {
            val needEnrollBiometricsFlow = flow {
                do {
                    val needEnrollBiometrics = runCatching(tangemSdkManager::needEnrollBiometrics).getOrNull()

                    if (needEnrollBiometrics != null) {
                        emit(needEnrollBiometrics)
                    }

                    delay(timeMillis = 200)
                } while (true)
            }

            needEnrollBiometricsFlow
                .distinctUntilChanged()
                .onEach { needEnrollBiometrics ->
                    store.dispatchWithMain(DetailsAction.AppSettings.BiometricsStatusChanged(needEnrollBiometrics))
                }
                .launchIn(lifecycleScope)
                .saveIn(checkBiometricsStatusJobHolder)
        }

        private fun enrollBiometrics() {
            Analytics.send(Settings.AppSettings.ButtonEnableBiometricAuthentication)
            activityResultCaller.openSystemBiometrySettings()
        }

        private fun changeAppThemeMode(appThemeMode: AppThemeMode) {
            val repository = store.inject(DaggerGraphState::appThemeModeRepository)

            scope.launch {
                repository.changeAppThemeMode(appThemeMode)
            }
        }

        private fun changeBalanceHiding(hideBalance: Boolean) {
            val repository = store.inject(DaggerGraphState::balanceHidingRepository)

            scope.launch {
                val newState = repository.getBalanceHidingSettings().copy(
                    isHidingEnabledInSettings = hideBalance,
                    isBalanceHidden = false,
                )

                repository.storeBalanceHidingSettings(newState)
            }
        }

        private fun toggleSaveWallets(state: DetailsState, enable: Boolean) = scope.launch {
            // Nothing to change
            val walletsRepository = store.inject(DaggerGraphState::walletsRepository)

            if (walletsRepository.shouldSaveUserWalletsSync() == enable) {
                store.dispatchWithMain(DetailsAction.AppSettings.SwitchPrivacySetting.Success)
                return@launch
            }

            toggleSaveWallets(state.scanResponse, enable)
                .doOnFailure {
                    store.dispatchWithMain(
                        DetailsAction.AppSettings.SwitchPrivacySetting.Failure(
                            prevState = !enable,
                            setting = AppSetting.SaveWallets,
                        ),
                    )
                }
                .doOnSuccess {
                    store.dispatchWithMain(DetailsAction.AppSettings.SwitchPrivacySetting.Success)
                }
        }

        private suspend fun toggleSaveWallets(scanResponse: ScanResponse?, enable: Boolean): CompletionResult<Unit> {
            return if (enable) {
                saveCurrentWallet(scanResponse, enableAccessCodesSaving = false)
            } else {
                deleteSavedWalletsAndAccessCodes()
            }
        }

        private fun toggleSaveAccessCodes(state: DetailsState, enable: Boolean) = scope.launch {
            val shouldSaveAccessCodes = store.inject(DaggerGraphState::settingsRepository).shouldSaveAccessCodes()

            // Nothing to change
            if (shouldSaveAccessCodes == enable) {
                store.dispatchWithMain(DetailsAction.AppSettings.SwitchPrivacySetting.Success)
                return@launch
            }

            toggleSaveAccessCodes(state.scanResponse, state.appSettingsState.saveWallets, enable)
                .doOnFailure {
                    store.dispatchWithMain(
                        DetailsAction.AppSettings.SwitchPrivacySetting.Failure(
                            prevState = !enable,
                            setting = AppSetting.SaveAccessCode,
                        ),
                    )
                }
                .doOnSuccess {
                    store.dispatchWithMain(DetailsAction.AppSettings.SwitchPrivacySetting.Success)
                }
        }

        private suspend fun toggleSaveAccessCodes(
            scanResponse: ScanResponse?,
            isWalletsSavingEnabled: Boolean,
            enable: Boolean,
        ): CompletionResult<Unit> {
            return if (enable) {
                if (!isWalletsSavingEnabled) {
                    saveCurrentWallet(scanResponse, enableAccessCodesSaving = true)
                } else {
                    saveAccessCodes(scanResponse)
                }
            } else {
                deleteSavedAccessCodes()
            }
        }

        private suspend fun saveCurrentWallet(
            scanResponse: ScanResponse?,
            enableAccessCodesSaving: Boolean,
        ): CompletionResult<Unit> {
            store.inject(DaggerGraphState::walletsRepository).saveShouldSaveUserWallets(item = true)

            return if (enableAccessCodesSaving) {
                saveAccessCodes(scanResponse)
            } else {
                CompletionResult.Success(Unit)
            }
                .doOnSuccess {
                    Analytics.send(Settings.AppSettings.SaveWalletSwitcherChanged(AnalyticsParam.OnOffState.On))
                }
                .doOnFailure { error ->
                    Timber.e(error, "Unable to save user wallet")
                }
        }

        private suspend fun deleteSavedWalletsAndAccessCodes(): CompletionResult<Unit> {
            Analytics.send(Settings.AppSettings.SaveWalletSwitcherChanged(AnalyticsParam.OnOffState.Off))

            deleteSavedAccessCodes()
            store.inject(DaggerGraphState::walletsRepository).saveShouldSaveUserWallets(item = false)

            store.dispatchNavigationAction { popTo<AppRoute.Home>() }

            return CompletionResult.Success(Unit)
        }

        private suspend fun saveAccessCodes(scanResponse: ScanResponse?): CompletionResult<Unit> {
            Analytics.send(Settings.AppSettings.SaveAccessCodeSwitcherChanged(AnalyticsParam.OnOffState.On))

            store.inject(DaggerGraphState::settingsRepository).setShouldSaveAccessCodes(value = true)

            store.inject(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
                isBiometricsRequestPolicy = scanResponse?.card?.isAccessCodeSet == true,
            )

            return CompletionResult.Success(Unit)
        }

        private suspend fun deleteSavedAccessCodes(): CompletionResult<Unit> {
            return tangemSdkManager.clearSavedUserCodes()
                .doOnSuccess {
                    Analytics.send(Settings.AppSettings.SaveAccessCodeSwitcherChanged(AnalyticsParam.OnOffState.Off))

                    store.inject(DaggerGraphState::settingsRepository).setShouldSaveAccessCodes(value = false)

                    store.inject(DaggerGraphState::cardSdkConfigRepository).setAccessCodeRequestPolicy(
                        isBiometricsRequestPolicy = false,
                    )
                }
                .doOnFailure { error ->
                    Timber.e(error, "Unable to delete saved access codes")
                }
        }
    }

    private fun scanAndSaveUserWallet() = scope.launch(Dispatchers.IO) {
        val cardSdkConfigRepository = store.inject(DaggerGraphState::cardSdkConfigRepository)

        val prevUseBiometricsForAccessCode = cardSdkConfigRepository.isBiometricsRequestPolicy()

        // Update access code policy for access code saving when a card was scanned
        val shouldSaveAccessCodes = store.inject(DaggerGraphState::settingsRepository).shouldSaveAccessCodes()
        cardSdkConfigRepository.setAccessCodeRequestPolicy(isBiometricsRequestPolicy = shouldSaveAccessCodes)

        store.inject(DaggerGraphState::scanCardProcessor).scan(
            analyticsSource = CoreAnalyticsParam.ScreensSources.Settings,
            onWalletNotCreated = {
                // No need to rollback policy, continue with the policy set before the card scan
                store.dispatchWithMain(DetailsAction.ScanAndSaveUserWallet.Success)
            },
            disclaimerWillShow = {
                store.dispatchNavigationAction(AppRouter::pop)
            },
            onSuccess = { scanResponse ->
                createUserWallet(scanResponse)
                    .doOnSuccess {
                        saveUserWalletAndPopBackToWalletScreen(
                            userWallet = it,
                            prevUseBiometricsForAccessCode = prevUseBiometricsForAccessCode,
                        )
                    }
                    .doOnFailure { error ->
                        Timber.e(error, "Unable to create user wallet")
                        handleError(error = error, prevUseBiometricsForAccessCode = prevUseBiometricsForAccessCode)
                    }
            },
            onFailure = { error ->
                Timber.e(error, "Unable to scan card")
                handleError(error = error, prevUseBiometricsForAccessCode = prevUseBiometricsForAccessCode)
            },
        )
    }

    private suspend fun createUserWallet(scanResponse: ScanResponse): CompletionResult<UserWallet> {
        val walletNameGenerateUseCase = store.inject(DaggerGraphState::generateWalletNameUseCase)
        val userWallet = UserWalletBuilder(scanResponse, walletNameGenerateUseCase).build()

        return if (userWallet != null) {
            CompletionResult.Success(userWallet)
        } else {
            CompletionResult.Failure(TangemSdkError.WalletIsNotCreated())
        }
    }

    private suspend fun saveUserWalletAndPopBackToWalletScreen(
        userWallet: UserWallet,
        prevUseBiometricsForAccessCode: Boolean,
    ) {
        val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)

        userWalletsListManager.save(userWallet)
            .doOnSuccess {
                store.onUserWalletSelected(userWallet)

                store.dispatchWithMain(DetailsAction.ScanAndSaveUserWallet.Success)
                store.dispatchNavigationAction { popTo<AppRoute.Wallet>() }
            }
            .doOnFailure { error ->
                if (error is UserWalletsListError.WalletAlreadySaved) {
                    userWalletsListManager.select(userWallet.walletId)
                    store.onUserWalletSelected(userWallet)

                    store.dispatchWithMain(DetailsAction.ScanAndSaveUserWallet.Success)
                    store.dispatchNavigationAction { popTo<AppRoute.Wallet>() }
                } else {
                    Timber.e(error, "Unable to create user wallet")
                    handleError(error, prevUseBiometricsForAccessCode)
                }
            }
    }

    private suspend fun handleError(error: TangemError, prevUseBiometricsForAccessCode: Boolean) {
        val cardSdkConfigRepository = store.inject(DaggerGraphState::cardSdkConfigRepository)

        // Rollback policy if card saving was failed
        cardSdkConfigRepository.setAccessCodeRequestPolicy(prevUseBiometricsForAccessCode)

        store.dispatchWithMain(DetailsAction.ScanAndSaveUserWallet.Error(error.toTextReference()))
    }

    private fun TangemError.toTextReference(): TextReference? {
        if (silent) return null

        return messageResId?.let(::resourceReference) ?: stringReference(customMessage)
    }
}