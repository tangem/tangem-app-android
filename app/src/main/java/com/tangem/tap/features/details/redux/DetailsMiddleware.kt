package com.tangem.tap.features.details.redux

import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.common.isTangemTwins
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.AnalyticsParam
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.onboarding.products.twins.redux.CreateTwinWalletMode
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.wallet.models.hasSendableAmountsOrPendingTransactions
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
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
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
                    store.dispatch(
                        GlobalAction.Onboarding.Start(
                            it,
                            fromHomeScreen = false,
                        ),
                    )
                }
            }
            DetailsAction.ScanCard -> {
                scope.launch {
                    when (val result = tangemSdkManager.scanCard()) {
                        is CompletionResult.Success -> {
                            val card = result.data
                            store.dispatchOnMain(DetailsAction.PrepareCardSettingsData(card))
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
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.ResetToFactory))
                }
                is DetailsAction.ResetToFactory.Proceed -> {
                    val card = store.state.detailsState.cardSettingsState?.card ?: return
                    if (card.isTangemTwins()) {
                        store.dispatch(DetailsAction.ReCreateTwinsWallet)
                        return
                    }
                    scope.launch {
                        val result = tangemSdkManager.resetToFactorySettings(card)
                        withContext(Dispatchers.Main) {
                            when (result) {
                                is CompletionResult.Success -> {
                                    currenciesRepository.removeCurrencies(card.cardId)
                                    store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
                                }
                                is CompletionResult.Failure -> {
                                    (result.error as? TangemSdkError)?.let { error ->
                                        store.state.globalState.analyticsHandlers?.logCardSdkError(
                                            error,
                                            Analytics.ActionToLog.PurgeWallet,
                                            card = store.state.detailsState.scanResponse?.card,
                                        )
                                    }
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
                            else -> null
                        }
                        withContext(Dispatchers.Main) {
                            when (result) {
                                is CompletionResult.Success -> {
                                    selectedOption?.let {
                                        store.dispatch(GlobalAction.UpdateSecurityOptions(it))
                                    }
                                    store.dispatch(NavigationAction.PopBackTo())
                                    store.dispatch(DetailsAction.ManageSecurity.SaveChanges.Success)
                                }
                                is CompletionResult.Failure -> {
                                    (result.error as? TangemSdkError)?.let { error ->
                                        store.state.globalState.analyticsHandlers?.logCardSdkError(
                                            error = error,
                                            actionToLog = Analytics.ActionToLog.ChangeSecOptions,
                                            parameters = mapOf(
                                                AnalyticsParam.NEW_SECURITY_OPTION to
                                                    (selectedOption?.name ?: ""),
                                            ),
                                            card = store.state.detailsState.scanResponse?.card,
                                        )
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
                        tangemSdkManager.setAccessCode(card.cardId)
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
// [REDACTED_TODO_COMMENT]
                }
            }
        }
    }
}

