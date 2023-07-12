package com.tangem.tap.features.details.redux

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.guard
import com.tangem.common.flatMap
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.domain.model.builders.UserWalletIdBuilder
import com.tangem.tap.domain.userWalletList.di.provideBiometricImplementation
import com.tangem.tap.domain.userWalletList.di.provideRuntimeImplementation
import com.tangem.tap.domain.userWalletList.isLockedSync
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.onboarding.products.twins.redux.CreateTwinWalletMode
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.tangemSdkManager
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

class DetailsMiddleware {
    private val eraseWalletMiddleware = EraseWalletMiddleware()
    private val manageSecurityMiddleware = ManageSecurityMiddleware()
    private val managePrivacyMiddleware = ManagePrivacyMiddleware()
    private val accessCodeRecoveryMiddleware = AccessCodeRecoveryMiddleware()
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
            is DetailsAction.ResetToFactory -> eraseWalletMiddleware.handle(action)
            is DetailsAction.ManageSecurity -> manageSecurityMiddleware.handle(action, state)
            is DetailsAction.AppSettings -> managePrivacyMiddleware.handle(state, action)
            is DetailsAction.ReCreateTwinsWallet -> {
                store.dispatch(TwinCardsAction.SetMode(CreateTwinWalletMode.RecreateWallet))
                store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingTwins))
            }
            is DetailsAction.AccessCodeRecovery -> accessCodeRecoveryMiddleware.handle(state, action)
            DetailsAction.ScanCard -> {
                scope.launch {
                    store.state.daggerGraphState.get(DaggerGraphState::scanCardProcessor)
                        .scan(allowsRequestAccessCodeFromRepository = true)
                        .doOnSuccess { scanResponse ->
                            // if we use biometric, scanResponse in GlobalState is null, and crashes NPE on twin cards
                            store.dispatch(GlobalAction.SaveScanResponse(scanResponse))
                            val currentUserWalletId = state.scanResponse
                                ?.let { UserWalletIdBuilder.scanResponse(it).build() }
                            val scannedUserWalletId = UserWalletIdBuilder.scanResponse(scanResponse)
                                .build()
                            val isSameWallet = currentUserWalletId == scannedUserWalletId

                            if (isSameWallet) {
                                store.dispatchOnMain(
                                    DetailsAction.PrepareCardSettingsData(
                                        scanResponse.card,
                                        scanResponse.cardTypesResolver,
                                    ),
                                )
                            } else {
                                store.dispatchDialogShow(
                                    AppDialog.SimpleOkDialogRes(
                                        headerId = R.string.common_warning,
                                        messageId = R.string.error_wrong_wallet_tapped,
                                    ),
                                )
                            }
                        }
                }
            }
        }
    }

    class EraseWalletMiddleware {
        fun handle(action: DetailsAction.ResetToFactory) {
            when (action) {
                is DetailsAction.ResetToFactory.Start -> {
                    val card = store.state.detailsState.cardSettingsState?.card ?: return
                    if (card.isTangemTwins) {
                        store.dispatch(DetailsAction.ReCreateTwinsWallet)
                        return
                    } else {
                        store.dispatch(NavigationAction.NavigateTo(AppScreen.ResetToFactory))
                    }
                }
                is DetailsAction.ResetToFactory.Proceed -> {
                    val card = store.state.detailsState.cardSettingsState?.card ?: return
                    scope.launch {
                        val userWalletId = UserWalletIdBuilder.card(card).build()

                        tangemSdkManager.resetToFactorySettings(card.cardId)
                            .flatMap { userWalletsListManager.delete(listOfNotNull(userWalletId)) }
                            .flatMap { tangemSdkManager.deleteSavedUserCodes(setOf(card.cardId)) }
                            .doOnSuccess {
                                Analytics.send(Settings.CardSettings.FactoryResetFinished())

                                val selectedUserWallet = userWalletsListManager.selectedUserWalletSync
                                if (selectedUserWallet != null) {
                                    if (userWalletsListManager.isLockedSync) {
                                        store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Welcome))
                                    } else {
                                        store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                                        store.onUserWalletSelected(selectedUserWallet)
                                    }
                                } else {
                                    store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Home))
                                }
                            }
                            .doOnFailure { error ->
                                if (error is TangemSdkError && error !is TangemSdkError.UserCancelled) {
                                    Analytics.send(Settings.CardSettings.FactoryResetFinished(error))
                                }
                            }
                    }
                }
                else -> Unit
            }
        }
    }

    class ManageSecurityMiddleware {
        @Suppress("ComplexMethod")
        fun handle(action: DetailsAction.ManageSecurity, detailsState: DetailsState) {
            when (action) {
                is DetailsAction.ManageSecurity.OpenSecurity -> {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.DetailsSecurity))
                }
                is DetailsAction.ManageSecurity.SaveChanges -> {
                    val cardSettingsState = detailsState.cardSettingsState
                    val cardId = cardSettingsState?.card?.cardId
                    val selectedOption = cardSettingsState?.manageSecurityState?.selectedOption
                    scope.launch {
                        val result = when (selectedOption) {
                            SecurityOption.LongTap -> tangemSdkManager.setLongTap(cardId)
                            SecurityOption.PassCode -> tangemSdkManager.setPasscode(cardId)
                            SecurityOption.AccessCode -> tangemSdkManager.setAccessCode(cardId)
                            else -> return@launch
                        }
                        withContext(Dispatchers.Main) {
                            val paramValue = AnalyticsParam.SecurityMode.from(selectedOption)
                            when (result) {
                                is CompletionResult.Success -> {
                                    Analytics.send(Settings.CardSettings.SecurityModeChanged(paramValue))
                                    store.dispatch(GlobalAction.UpdateSecurityOptions(selectedOption))
                                    store.dispatch(NavigationAction.PopBackTo())
                                    store.dispatch(DetailsAction.ManageSecurity.SaveChanges.Success)
                                }
                                is CompletionResult.Failure -> {
                                    val error = result.error
                                    if (error is TangemSdkError && error !is TangemSdkError.UserCancelled) {
                                        Analytics.send(Settings.CardSettings.SecurityModeChanged(paramValue, error))
                                    }
                                    store.dispatch(DetailsAction.ManageSecurity.SaveChanges.Failure)
                                }
                                else -> Unit
                            }
                        }
                    }
                }
                is DetailsAction.ManageSecurity.ChangeAccessCode -> {
                    val card = store.state.detailsState.cardSettingsState?.card ?: return
                    scope.launch {
                        when (tangemSdkManager.setAccessCode(card.cardId)) {
                            is CompletionResult.Success -> Analytics.send(Settings.CardSettings.UserCodeChanged())
                            is CompletionResult.Failure -> {}
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    class ManagePrivacyMiddleware {
        fun handle(state: DetailsState, action: DetailsAction.AppSettings) {
            when (action) {
                is DetailsAction.AppSettings.SwitchPrivacySetting -> {
                    when (action.setting) {
                        AppSetting.SaveWallets -> toggleSaveWallets(state, enable = action.enable)
                        AppSetting.SaveAccessCode -> toggleSaveAccessCodes(state, enable = action.enable)
                    }
                }
                is DetailsAction.AppSettings.CheckBiometricsStatus -> {
                    checkBiometricsStatus(action.awaitStatusChange, state)
                }
                is DetailsAction.AppSettings.EnrollBiometrics -> {
                    enrollBiometrics()
                }
                is DetailsAction.AppSettings.SwitchPrivacySetting.Success,
                is DetailsAction.AppSettings.SwitchPrivacySetting.Failure,
                is DetailsAction.AppSettings.BiometricsStatusChanged,
                -> Unit
            }
        }

        /**
         * @param awaitStatusChange If true then start a new coroutine and check the biometric status every 100
         * milliseconds until it changes
         * */
        private fun checkBiometricsStatus(awaitStatusChange: Boolean, state: DetailsState) {
            scope.launch {
                if (awaitStatusChange) {
                    while (state.appSettingsState.needEnrollBiometrics == tangemSdkManager.needEnrollBiometrics) {
                        delay(timeMillis = 100)
                    }
                }
                store.dispatchWithMain(
                    DetailsAction.AppSettings.BiometricsStatusChanged(
                        needEnrollBiometrics = tangemSdkManager.needEnrollBiometrics,
                    ),
                )
            }
        }

        private fun enrollBiometrics() {
            Analytics.send(Settings.AppSettings.ButtonEnableBiometricAuthentication)
            store.dispatchOnMain(NavigationAction.OpenBiometricsSettings)
        }

        private fun toggleSaveWallets(state: DetailsState, enable: Boolean) = scope.launch {
            // Nothing to change
            if (preferencesStorage.shouldSaveUserWallets == enable) {
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
            // Nothing to change
            if (preferencesStorage.shouldSaveAccessCodes == enable) {
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
            val userWallet = userWalletsListManager.selectedUserWalletSync
                ?: scanResponse?.let { UserWalletBuilder(it).build() }
                ?: return CompletionResult.Failure(
                    error = TangemSdkError.ExceptionError(IllegalStateException("scanResponse is null")),
                )

            updateUserWalletsListManager(enableUserWalletsSaving = true)

            return userWalletsListManager.save(userWallet)
                .flatMap {
                    if (enableAccessCodesSaving) {
                        saveAccessCodes(scanResponse)
                    } else {
                        CompletionResult.Success(Unit)
                    }
                }
                .doOnSuccess {
                    Analytics.send(Settings.AppSettings.SaveWalletSwitcherChanged(AnalyticsParam.OnOffState.On))

                    preferencesStorage.shouldShowSaveUserWalletScreen = false
                    preferencesStorage.shouldSaveUserWallets = true

                    store.dispatchWithMain(WalletAction.UpdateCanSaveUserWallets(canSaveUserWallets = true))
                }
                .doOnFailure { error ->
                    Timber.e(error, "Unable to save user wallet")
                }
        }

        private suspend fun deleteSavedWalletsAndAccessCodes(): CompletionResult<Unit> {
            return userWalletsListManager.clear()
                .flatMap { walletStoresManager.clear() }
                .doOnSuccess {
                    Analytics.send(Settings.AppSettings.SaveWalletSwitcherChanged(AnalyticsParam.OnOffState.Off))
                    deleteSavedAccessCodes()
                    updateUserWalletsListManager(enableUserWalletsSaving = false)
                    preferencesStorage.shouldSaveUserWallets = false

                    store.dispatchWithMain(WalletAction.UpdateCanSaveUserWallets(canSaveUserWallets = true))
                    store.dispatchWithMain(NavigationAction.PopBackTo(AppScreen.Home))
                }
                .doOnFailure { error ->
                    Timber.e(error, "Unable to delete saved wallets")
                }
        }

        private fun saveAccessCodes(scanResponse: ScanResponse?): CompletionResult<Unit> {
            Analytics.send(Settings.AppSettings.SaveAccessCodeSwitcherChanged(AnalyticsParam.OnOffState.On))

            preferencesStorage.shouldSaveAccessCodes = true
            tangemSdkManager.setAccessCodeRequestPolicy(
                useBiometricsForAccessCode = scanResponse?.card?.isAccessCodeSet == true,
            )

            return CompletionResult.Success(Unit)
        }

        private suspend fun deleteSavedAccessCodes(): CompletionResult<Unit> {
            return tangemSdkManager.clearSavedUserCodes()
                .doOnSuccess {
                    Analytics.send(Settings.AppSettings.SaveAccessCodeSwitcherChanged(AnalyticsParam.OnOffState.Off))

                    preferencesStorage.shouldSaveAccessCodes = false
                    tangemSdkManager.setAccessCodeRequestPolicy(
                        useBiometricsForAccessCode = false,
                    )
                }
                .doOnFailure { error ->
                    Timber.e(error, "Unable to delete saved access codes")
                }
        }

        private suspend fun updateUserWalletsListManager(enableUserWalletsSaving: Boolean) {
            val manager = if (enableUserWalletsSaving) {
                createBiometricsUserWalletsManager() ?: return
            } else {
                UserWalletsListManager.provideRuntimeImplementation()
            }

            store.dispatchWithMain(GlobalAction.UpdateUserWalletsListManager(manager))
        }

        private fun createBiometricsUserWalletsManager(): UserWalletsListManager? {
            val context = foregroundActivityObserver.foregroundActivity?.applicationContext.guard {
                Timber.e(IllegalStateException("No activities in foreground"))
                return null
            }

            return UserWalletsListManager.provideBiometricImplementation(
                context = context,
                tangemSdkManager = tangemSdkManager,
            )
        }
    }

    class AccessCodeRecoveryMiddleware {
        fun handle(state: DetailsState, action: DetailsAction.AccessCodeRecovery) {
            when (action) {
                is DetailsAction.AccessCodeRecovery.Open -> {
                    Analytics.send(Settings.CardSettings.AccessCodeRecoveryButton())
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.AccessCodeRecovery))
                }
                is DetailsAction.AccessCodeRecovery.SaveChanges -> {
                    scope.launch {
                        tangemSdkManager
                            .setAccessCodeRecoveryEnabled(state.cardSettingsState?.card?.cardId, action.enabled)
                            .doOnSuccess {
                                Analytics.send(
                                    Settings.CardSettings.AccessCodeRecoveryChanged(
                                        AnalyticsParam.AccessCodeRecoveryStatus.from(action.enabled),
                                    ),
                                )
                                store.dispatchOnMain(NavigationAction.PopBackTo())
                                store.dispatchOnMain(
                                    DetailsAction.AccessCodeRecovery.SaveChanges.Success(action.enabled),
                                )
                            }
                    }
                }
                is DetailsAction.AccessCodeRecovery.SelectOption -> Unit
                is DetailsAction.AccessCodeRecovery.SaveChanges.Success -> Unit
            }
        }
    }
}