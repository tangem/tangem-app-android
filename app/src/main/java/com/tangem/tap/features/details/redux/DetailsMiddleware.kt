package com.tangem.tap.features.details.redux

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.extensions.getUserWalletId
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.onboarding.products.twins.redux.CreateTwinWalletMode
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.wallet.models.hasSendableAmountsOrPendingTransactions
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Action
import org.rekotlin.Middleware

class DetailsMiddleware {
    private val eraseWalletMiddleware = EraseWalletMiddleware()
    private val manageSecurityMiddleware = ManageSecurityMiddleware()
    private val managePrivacyMiddleware = ManagePrivacyMiddleware()
    val detailsMiddleware: Middleware<AppState> = { _, state ->
        { next ->
            { action ->
                handleAction(state, action)
                next(action)
            }
        }
    }

    private fun handleAction(state: () -> AppState?, action: Action) {
        if (DemoHelper.tryHandle(state, action)) return

        when (action) {
            is DetailsAction.ResetToFactory -> eraseWalletMiddleware.handle(action)
            is DetailsAction.ManageSecurity -> manageSecurityMiddleware.handle(action)
            is DetailsAction.AppSettings -> managePrivacyMiddleware.handle(action)
            is DetailsAction.ShowDisclaimer -> {
                val uri = store.state.detailsState.cardTermsOfUseUrl
                if (uri != null) {
                    store.dispatch(NavigationAction.OpenDocument(uri))
                }
            }
            is DetailsAction.ReCreateTwinsWallet -> {
                val wallet =
                    store.state.walletState.walletManagers.map { it.wallet }.firstOrNull()
                if (wallet == null) {
                    store.dispatch(TwinCardsAction.SetMode(CreateTwinWalletMode.RecreateWallet))
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingTwins))
                } else {
                    if (wallet.hasSendableAmountsOrPendingTransactions()) {
                        val walletIsNotEmpty =
                            store.state.globalState.resources.strings.walletIsNotEmpty
                        store.dispatchNotification(walletIsNotEmpty)
                    } else {
                        store.dispatch(TwinCardsAction.SetMode(CreateTwinWalletMode.RecreateWallet))
                        store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingTwins))
                    }
                }
            }
            is DetailsAction.CreateBackup -> {
                store.state.detailsState.scanResponse?.let {
                    store.dispatch(GlobalAction.Onboarding.Start(it, canSkipBackup = false))
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
                }
            }
            DetailsAction.ScanCard -> {
                scope.launch {
                    when (val result = tangemSdkManager.scanCard()) {
                        is CompletionResult.Success -> {
                            val card = result.data
                            if (card.getUserWalletId() ==
                                store.state.globalState.scanResponse?.card?.getUserWalletId()
                            ) {
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
                        is CompletionResult.Failure -> {
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
                        val result = tangemSdkManager.resetToFactorySettings(card)
                        when (result) {
                            is CompletionResult.Success -> {
                                Analytics.send(Settings.CardSettings.FactoryResetFinished())
                                store.dispatchOnMain(NavigationAction.PopBackTo(AppScreen.Home))
                            }
                            is CompletionResult.Failure -> {
                                val error = result.error
                                if (error is TangemSdkError && error !is TangemSdkError.UserCancelled) {
                                    Analytics.send(Settings.CardSettings.FactoryResetFinished(error))
                                }
                            }
                        }
                    }
                }
                else -> { /* no-op */
                }
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
                                    val error = result.error
                                    if (error is TangemSdkError && error !is TangemSdkError.UserCancelled) {
                                        Analytics.send(Settings.CardSettings.SecurityModeChanged(paramValue, error))
                                    }
                                    store.dispatch(DetailsAction.ManageSecurity.SaveChanges.Failure)
                                }
                                else -> { /* no-op */
                                }
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
                else -> { /* no-op */
                }
            }
        }
    }

    class ManagePrivacyMiddleware {
        fun handle(action: DetailsAction.AppSettings) {
            when (action) {
                is DetailsAction.AppSettings.SwitchPrivacySetting -> {
//                    TODO()
                }
            }
        }
    }
}
