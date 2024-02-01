package com.tangem.tap.features.details.redux

import androidx.lifecycle.LifecycleCoroutineScope
import com.tangem.common.*
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.Basic
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.userwallets.UserWalletIdBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.isLockedSync
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
import com.tangem.tap.domain.userWalletList.di.provideBiometricImplementation
import com.tangem.tap.domain.userWalletList.di.provideRuntimeImplementation
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.onboarding.products.twins.redux.CreateTwinWalletMode
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber
import com.tangem.core.analytics.models.AnalyticsParam as CoreAnalyticsParam

class DetailsMiddleware {
    private val eraseWalletMiddleware = EraseWalletMiddleware()
    private val manageSecurityMiddleware = ManageSecurityMiddleware()
    private val appSettingsMiddleware = AppSettingsMiddleware()
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
            is DetailsAction.AppSettings -> appSettingsMiddleware.handle(state, action)
            is DetailsAction.ReCreateTwinsWallet -> {
                store.dispatch(TwinCardsAction.SetMode(CreateTwinWalletMode.RecreateWallet))
                store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingTwins))
            }
            is DetailsAction.AccessCodeRecovery -> accessCodeRecoveryMiddleware.handle(state, action)
            is DetailsAction.ScanCard -> scanCard(state)
            is DetailsAction.ScanAndSaveUserWallet -> scanAndSaveUserWallet()
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

                        // we must require a password regardless of biometric settings
                        val policy = tangemSdkManager.userCodeRequestPolicy
                        val doBeforeErase = {
                            val type = if (card.isAccessCodeSet) {
                                UserCodeType.AccessCode
                            } else if (card.isPasscodeSet == true) {
                                UserCodeType.Passcode
                            } else {
                                null
                            }

                            type?.let {
                                tangemSdkManager.setUserCodeRequestPolicy(UserCodeRequestPolicy.Always(type))
                            }
                        }

                        val doAfterErase = {
                            tangemSdkManager.setUserCodeRequestPolicy(policy)
                        }

                        doBeforeErase()
                        tangemSdkManager.resetToFactorySettings(card.cardId, true)
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
                            .doOnResult {
                                doAfterErase()
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
                    observeBiometricsStatusChanges(state, action.lifecycleScope)
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
                -> Unit
            }
        }

        private fun observeBiometricsStatusChanges(state: DetailsState, lifecycleScope: LifecycleCoroutineScope) {
            lifecycleScope.launch(Dispatchers.IO) {
                do {
                    val needEnrollBiometrics = runCatching(tangemSdkManager::needEnrollBiometrics).getOrNull()

                    if (needEnrollBiometrics != null &&
                        needEnrollBiometrics != state.appSettingsState.needEnrollBiometrics
                    ) {
                        store.dispatchWithMain(DetailsAction.AppSettings.BiometricsStatusChanged(needEnrollBiometrics))
                    }

                    delay(timeMillis = 500)
                } while (true)
            }.saveIn(checkBiometricsStatusJobHolder)
        }

        private fun enrollBiometrics() {
            Analytics.send(Settings.AppSettings.ButtonEnableBiometricAuthentication)
            store.dispatchOnMain(NavigationAction.OpenBiometricsSettings)
        }

        private fun changeAppThemeMode(appThemeMode: AppThemeMode) {
            val repository = store.state.daggerGraphState.get(DaggerGraphState::appThemeModeRepository)

            scope.launch {
                repository.changeAppThemeMode(appThemeMode)

                store.dispatchWithMain(GlobalAction.ChangeAppThemeMode(appThemeMode))
            }
        }

        private fun changeBalanceHiding(hideBalance: Boolean) {
            val repository = store.state.daggerGraphState.get(DaggerGraphState::balanceHidingRepository)

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
            val walletsRepository = store.state.daggerGraphState.get(DaggerGraphState::walletsRepository)
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
                    store.state.daggerGraphState.get(DaggerGraphState::walletsRepository)
                        .saveShouldSaveUserWallets(item = true)
                }
                .doOnFailure { error ->
                    Timber.e(error, "Unable to save user wallet")
                }
        }

        private suspend fun deleteSavedWalletsAndAccessCodes(): CompletionResult<Unit> {
            return userWalletsListManager.clear()
                .doOnSuccess {
                    Analytics.send(Settings.AppSettings.SaveWalletSwitcherChanged(AnalyticsParam.OnOffState.Off))
                    deleteSavedAccessCodes()
                    updateUserWalletsListManager(enableUserWalletsSaving = false)
                    store.state.daggerGraphState.get(DaggerGraphState::walletsRepository)
                        .saveShouldSaveUserWallets(item = false)

                    store.dispatchWithMain(NavigationAction.PopBackTo(AppScreen.Home))
                }
                .doOnFailure { error ->
                    Timber.e(error, "Unable to delete saved wallets")
                }
        }

        private fun saveAccessCodes(scanResponse: ScanResponse?): CompletionResult<Unit> {
            Analytics.send(Settings.AppSettings.SaveAccessCodeSwitcherChanged(AnalyticsParam.OnOffState.On))

            preferencesStorage.shouldSaveAccessCodes = true
            store.state.daggerGraphState
                .get(DaggerGraphState::cardSdkConfigRepository)
                .setAccessCodeRequestPolicy(isBiometricsRequestPolicy = scanResponse?.card?.isAccessCodeSet == true)

            return CompletionResult.Success(Unit)
        }

        private suspend fun deleteSavedAccessCodes(): CompletionResult<Unit> {
            return tangemSdkManager.clearSavedUserCodes()
                .doOnSuccess {
                    Analytics.send(Settings.AppSettings.SaveAccessCodeSwitcherChanged(AnalyticsParam.OnOffState.Off))

                    preferencesStorage.shouldSaveAccessCodes = false
                    store.state.daggerGraphState
                        .get(DaggerGraphState::cardSdkConfigRepository)
                        .setAccessCodeRequestPolicy(isBiometricsRequestPolicy = false)
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

            return UserWalletsListManager.provideBiometricImplementation(context)
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

    private fun scanCard(state: DetailsState) = scope.launch {
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

    private fun scanAndSaveUserWallet() = scope.launch(Dispatchers.IO) {
        val cardSdkConfigRepository = store.state.daggerGraphState.get(DaggerGraphState::cardSdkConfigRepository)

        val prevUseBiometricsForAccessCode = cardSdkConfigRepository.isBiometricsRequestPolicy()

        // Update access code policy for access code saving when a card was scanned
        cardSdkConfigRepository.setAccessCodeRequestPolicy(
            isBiometricsRequestPolicy = preferencesStorage.shouldSaveAccessCodes,
        )

        store.state.daggerGraphState.get(DaggerGraphState::scanCardProcessor).scan(
            analyticsEvent = Basic.CardWasScanned(CoreAnalyticsParam.ScannedFrom.MyWallets),
            onWalletNotCreated = {
                // No need to rollback policy, continue with the policy set before the card scan
                store.dispatchWithMain(DetailsAction.ScanAndSaveUserWallet.Success)
            },
            disclaimerWillShow = {
                store.dispatchOnMain(NavigationAction.PopBackTo())
            },
            onSuccess = { scanResponse ->
                saveUserWalletAndPopBackToWalletScreen(scanResponse)
                    .doOnFailure { error ->
                        // Rollback policy if card saving was failed
                        cardSdkConfigRepository.setAccessCodeRequestPolicy(prevUseBiometricsForAccessCode)
                        Timber.e(error, "Unable to save user wallet")

                        store.dispatchWithMain(DetailsAction.ScanAndSaveUserWallet.Error(error.toTextReference()))
                    }
            },
            onFailure = { error ->
                // Rollback policy if card scanning was failed
                cardSdkConfigRepository.setAccessCodeRequestPolicy(prevUseBiometricsForAccessCode)
                Timber.e(error, "Unable to scan card")
                store.dispatchWithMain(DetailsAction.ScanAndSaveUserWallet.Error(error.toTextReference()))
            },
        )
    }

    private suspend fun saveUserWalletAndPopBackToWalletScreen(scanResponse: ScanResponse): CompletionResult<Unit> {
        val userWallet = UserWalletBuilder(scanResponse).build()
            ?: return CompletionResult.Failure(TangemSdkError.WalletIsNotCreated())

        return userWalletsListManager.save(userWallet)
            .doOnSuccess {
                store.onUserWalletSelected(userWallet)

                store.dispatchWithMain(DetailsAction.ScanAndSaveUserWallet.Success)
                store.dispatchWithMain(NavigationAction.PopBackTo(AppScreen.Wallet))
            }
    }

    private fun TangemError.toTextReference(): TextReference? {
        if (silent) return null

        return messageResId?.let(::resourceReference) ?: stringReference(customMessage)
    }
}
