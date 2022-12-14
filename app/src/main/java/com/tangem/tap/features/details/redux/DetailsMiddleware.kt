package com.tangem.tap.features.details.redux

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.flatMap
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.common.util.userWalletId
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.onUserWalletSelected
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.model.builders.UserWalletBuilder
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.onboarding.products.twins.redux.CreateTwinWalletMode
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletStoresManager
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
            is DetailsAction.ManageSecurity -> manageSecurityMiddleware.handle(action)
            is DetailsAction.AppSettings -> managePrivacyMiddleware.handle(state, action)
            is DetailsAction.ShowDisclaimer -> {
                val uri = store.state.detailsState.cardTermsOfUseUrl
                if (uri != null) {
                    store.dispatch(NavigationAction.OpenDocument(uri))
                }
            }
            is DetailsAction.ReCreateTwinsWallet -> {
                store.dispatch(TwinCardsAction.SetMode(CreateTwinWalletMode.RecreateWallet))
                store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingTwins))
            }
            is DetailsAction.CreateBackup -> {
                store.state.detailsState.scanResponse?.let {
                    store.dispatch(GlobalAction.Onboarding.Start(it, canSkipBackup = false))
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
                }
            }
            DetailsAction.ScanCard -> {
                scope.launch {
                    tangemSdkManager.scanCard(cardId = state.scanResponse?.card?.cardId)
                        .doOnSuccess { card ->
                            val currentCardId = store.state.globalState.scanResponse?.card
                                ?.userWalletId
                                ?.stringValue
                            if (card.userWalletId.stringValue == currentCardId) {
                                store.dispatchOnMain(DetailsAction.PrepareCardSettingsData(card))
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
                        tangemSdkManager.resetToFactorySettings(card.cardId)
                            .flatMap { userWalletsListManager.delete(listOf(card.userWalletId)) }
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
                                    userWalletsListManager.lock()
                                    store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Home))
                                }
                            }
                            .doOnFailure { error ->
                                (error as? TangemSdkError)?.let { sdkError ->
                                    Analytics.send(Settings.CardSettings.FactoryResetFinished(sdkError))
                                }
                            }
                    }
                }
                else -> Unit
            }
        }
    }

    class ManageSecurityMiddleware {
        fun handle(action: DetailsAction.ManageSecurity) {
            when (action) {
                is DetailsAction.ManageSecurity.OpenSecurity -> {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.DetailsSecurity))
                }
                is DetailsAction.ManageSecurity.SaveChanges -> {
                    val cardId = store.state.detailsState.scanResponse?.card?.cardId
                    val selectedOption =
                        store.state.detailsState.cardSettingsState?.manageSecurityState?.selectedOption
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
                                    (result.error as? TangemSdkError)?.let { error ->
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
                        PrivacySetting.SaveWallets -> toggleSaveWallets(state, enable = action.enable)
                        PrivacySetting.SaveAccessCode -> toggleSaveAccessCodes(state, enable = action.enable)
                    }
                }
                is DetailsAction.AppSettings.CheckBiometricsStatus -> {
                    checkBiometricsStatus(action.awaitStatusChange, state)
                }
                is DetailsAction.AppSettings.EnrollBiometrics -> {
                    enrollBiometrics()
                }
                is DetailsAction.AppSettings.SwitchPrivacySetting.Success,
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
                    while (state.needEnrollBiometrics == tangemSdkManager.needEnrollBiometrics) {
                        delay(timeMillis = 100)
                    }
                }
                store.dispatchOnMain(
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
            if (state.saveWallets == enable) return@launch
            if (enable) {
                saveCurrentWallet(state)
            } else {
                deleteSavedWallets()
                if (state.saveAccessCodes) {
                    deleteSavedAccessCodes()
                }
            }
        }

        private fun toggleSaveAccessCodes(state: DetailsState, enable: Boolean) = scope.launch {
            if (state.saveAccessCodes == enable) return@launch
            if (enable) {
                if (!state.saveWallets) {
                    saveCurrentWallet(state)
                }
                saveAccessCodes(state)
            } else {
                deleteSavedAccessCodes()
            }
        }

        private suspend fun saveCurrentWallet(state: DetailsState) {
            val scanResponse = state.scanResponse ?: return
            val userWallet = UserWalletBuilder(scanResponse).build()

            userWalletsListManager.save(userWallet)
                .doOnFailure { error ->
                    Timber.e(error, "Wallet saving failed")
                }
                .doOnSuccess {
                    Analytics.send(Settings.AppSettings.SaveWalletSwitcherChanged(AnalyticsParam.OnOffState.On))

                    preferencesStorage.shouldShowSaveUserWalletScreen = false
                    preferencesStorage.shouldSaveUserWallets = true

                    store.dispatchOnMain(
                        DetailsAction.AppSettings.SwitchPrivacySetting.Success(
                            setting = PrivacySetting.SaveWallets,
                            enable = true,
                        ),
                    )

                    store.onUserWalletSelected(userWallet)
                }
        }

        private suspend fun deleteSavedWallets() {
            userWalletsListManager.clear()
                .flatMap { walletStoresManager.clear() }
                .doOnSuccess {
                    Analytics.send(Settings.AppSettings.SaveWalletSwitcherChanged(AnalyticsParam.OnOffState.Off))

                    preferencesStorage.shouldSaveUserWallets = false

                    store.dispatchOnMain(
                        DetailsAction.AppSettings.SwitchPrivacySetting.Success(
                            setting = PrivacySetting.SaveWallets,
                            enable = false,
                        ),
                    )

                    store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Home))
                }
        }

        private fun saveAccessCodes(state: DetailsState) {
            Analytics.send(Settings.AppSettings.SaveAccessCodeSwitcherChanged(AnalyticsParam.OnOffState.On))

            preferencesStorage.shouldSaveAccessCodes = true
            tangemSdkManager.setAccessCodeRequestPolicy(
                useBiometricsForAccessCode = state.scanResponse?.card?.isAccessCodeSet == true,
            )

            store.dispatchOnMain(
                DetailsAction.AppSettings.SwitchPrivacySetting.Success(
                    setting = PrivacySetting.SaveAccessCode,
                    enable = true,
                ),
            )
        }

        private suspend fun deleteSavedAccessCodes() {
            tangemSdkManager.clearSavedUserCodes()
                .doOnSuccess {
                    Analytics.send(Settings.AppSettings.SaveAccessCodeSwitcherChanged(AnalyticsParam.OnOffState.Off))

                    preferencesStorage.shouldSaveAccessCodes = false
                    tangemSdkManager.setAccessCodeRequestPolicy(
                        useBiometricsForAccessCode = false,
                    )

                    store.dispatchOnMain(
                        DetailsAction.AppSettings.SwitchPrivacySetting.Success(
                            setting = PrivacySetting.SaveAccessCode,
                            enable = false,
                        ),
                    )
                }
        }
    }
}
