package com.tangem.tap.features.details.redux

import androidx.lifecycle.LifecycleCoroutineScope
import com.tangem.common.*
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.onboarding.products.twins.redux.CreateTwinWalletMode
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
        @Suppress("CyclomaticComplexMethod")
        fun handle(action: DetailsAction.ResetToFactory) {
            val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)

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
                            .flatMap {
                                userWalletsListManager.delete(listOfNotNull(userWalletId))
                            }
                            .flatMap {
                                tangemSdkManager.deleteSavedUserCodes(setOf(card.cardId))
                            }
                            .doOnSuccess {
                                Analytics.send(Settings.CardSettings.FactoryResetFinished())

                                val selectedUserWallet = userWalletsListManager.selectedUserWalletSync
                                if (selectedUserWallet != null) {
                                    store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Wallet))
                                    store.onUserWalletSelected(selectedUserWallet)
                                } else {
                                    val isLocked = runCatching { userWalletsListManager.asLockable()?.isLockedSync }
                                        .fold(onSuccess = { true }, onFailure = { false })
                                    if (isLocked && userWalletsListManager.hasUserWallets) {
                                        store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Welcome))
                                    } else {
                                        store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Home))
                                    }
                                }
                            }
                            .doOnFailure { error ->
                                if (error is TangemSdkError && error !is TangemSdkError.UserCancelled) {
                                    Analytics.send(Settings.CardSettings.FactoryResetFinished(error = error))
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
            store.dispatchOnMain(NavigationAction.OpenBiometricsSettings)
        }

        private fun changeAppThemeMode(appThemeMode: AppThemeMode) {
            val repository = store.inject(DaggerGraphState::appThemeModeRepository)

            scope.launch {
                repository.changeAppThemeMode(appThemeMode)

                store.dispatchWithMain(GlobalAction.ChangeAppThemeMode(appThemeMode))
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

            store.dispatchWithMain(NavigationAction.PopBackTo(AppScreen.Home))

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
        store.inject(DaggerGraphState::scanCardProcessor)
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
                        DetailsAction.PrepareCardSettingsData(scanResponse = scanResponse),
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
                store.dispatchOnMain(NavigationAction.PopBackTo())
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
                store.dispatchWithMain(NavigationAction.PopBackTo(AppScreen.Wallet))
            }
            .doOnFailure { error ->
                if (error is UserWalletsListError.WalletAlreadySaved) {
                    userWalletsListManager.select(userWallet.walletId)
                    store.onUserWalletSelected(userWallet)

                    store.dispatchWithMain(DetailsAction.ScanAndSaveUserWallet.Success)
                    store.dispatchWithMain(NavigationAction.PopBackTo(AppScreen.Wallet))
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