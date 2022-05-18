package com.tangem.tap.features.details.redux

import com.tangem.common.CompletionResult
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.TangemSdkError
import com.tangem.common.services.Result
import com.tangem.network.api.tangemTech.CurrenciesResponse
import com.tangem.operations.pins.CheckUserCodesResponse
import com.tangem.tap.*
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.AnalyticsParam
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.onboarding.products.twins.redux.CreateTwinWalletMode
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.wallet.models.hasSendableAmountsOrPendingTransactions
import com.tangem.tap.features.wallet.redux.WalletAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Middleware

class DetailsMiddleware {
    private val eraseWalletMiddleware = EraseWalletMiddleware()
    private val appCurrencyMiddleware = AppCurrencyMiddleware()
    private val manageSecurityMiddleware = ManageSecurityMiddleware()
    val detailsMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                when (action) {
                    is DetailsAction.PrepareScreen -> prepareData(action)
                    is DetailsAction.ResetToFactory -> eraseWalletMiddleware.handle(action)
                    is DetailsAction.AppCurrencyAction -> appCurrencyMiddleware.handle(action)
                    is DetailsAction.ManageSecurity -> manageSecurityMiddleware.handle(action)
                    is DetailsAction.ShowDisclaimer -> {
                        store.dispatch(DisclaimerAction.ShowAcceptedDisclaimer)
                        store.dispatch(NavigationAction.NavigateTo(AppScreen.Disclaimer))
                    }
                    is DetailsAction.ReCreateTwinsWallet -> {
                        val wallet = store.state.walletState.walletManagers.map { it.wallet }.firstOrNull()
                        if (wallet == null) {
                            store.dispatch(TwinCardsAction.SetMode(CreateTwinWalletMode.RecreateWallet))
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingTwins))
                        } else {
                            if (wallet.hasSendableAmountsOrPendingTransactions()) {
                                val walletIsNotEmpty = store.state.globalState.resources.strings.walletIsNotEmpty
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
                            store.dispatch(GlobalAction.Onboarding.Start(it, fromHomeScreen = false))
                        }
                    }
                }
                next(action)
            }
        }
    }

    private fun prepareData(action: DetailsAction.PrepareScreen) {
        val fiatCurrenciesPrefStorage = preferencesStorage.fiatCurrenciesPrefStorage
        val storedFiatCurrencies = fiatCurrenciesPrefStorage.restore()
        if (storedFiatCurrencies.isNotEmpty()) {
            store.dispatch(
                DetailsAction.AppCurrencyAction.SetCurrencies(
                    currencies = storedFiatCurrencies.mapToUiModel()
                )
            )
        }

        scope.launch {
            val tangemTechService = action.tangemTechService
            when (val result = tangemTechService.currencies()) {
                is Result.Success -> {
                    val currenciesList = result.data.currencies
                    if (currenciesList.isNotEmpty() &&
                        currenciesList.toSet() != storedFiatCurrencies.toSet()
                    ) {
                        fiatCurrenciesPrefStorage.save(currenciesList)
                        dispatchOnMain(
                            DetailsAction.AppCurrencyAction.SetCurrencies(
                                currencies = currenciesList.mapToUiModel()
                            )
                        )
                    }
                }
                is Result.Failure -> {}
            }
        }
    }

    private fun List<CurrenciesResponse.Currency>.mapToUiModel(): List<FiatCurrency> {
        return this.map {
            FiatCurrency(
                code = it.code,
                name = it.name,
                symbol = it.unit
            )
        }
    }

    class EraseWalletMiddleware {
        fun handle(action: DetailsAction.ResetToFactory) {
            when (action) {
                is DetailsAction.ResetToFactory.Proceed -> {
                    when (store.state.detailsState.eraseWalletState) {
                        EraseWalletState.Allowed ->
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.DetailsConfirm))
                        EraseWalletState.NotAllowedByCard ->
                            store.dispatch(DetailsAction.ResetToFactory.Proceed.NotAllowedByCard)
                        EraseWalletState.NotEmpty ->
                            store.dispatch(DetailsAction.ResetToFactory.Proceed.NotEmpty)
                    }
                }
                is DetailsAction.ResetToFactory.Cancel -> {
                    store.dispatch(NavigationAction.PopBackTo())
                }
                is DetailsAction.ResetToFactory.Confirm -> {
                    val card = store.state.detailsState.scanResponse?.card ?: return
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
                                            card = store.state.detailsState.scanResponse?.card
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    class AppCurrencyMiddleware {
        fun handle(action: DetailsAction.AppCurrencyAction) {
            when (action) {
                is DetailsAction.AppCurrencyAction.SelectAppCurrency -> {
                    store.state.globalState.tapWalletManager.rates.clear()
                    preferencesStorage.fiatCurrenciesPrefStorage
                        .saveAppCurrency(action.fiatCurrency)
                    store.dispatch(GlobalAction.ChangeAppCurrency(action.fiatCurrency))
                    store.dispatch(WalletAction.LoadFiatRate())
                }
            }
        }
    }

    class ManageSecurityMiddleware {
        fun handle(action: DetailsAction.ManageSecurity) {
            when (action) {
                is DetailsAction.ManageSecurity.CheckCurrentSecurityOption -> {
                    if (action.card.firmwareVersion >= FirmwareVersion.IsAccessCodeStatusAvailable) {
                        // for a card that meets this condition, we can get these statuses from it
                        val simulatedResponse = CheckUserCodesResponse(
                            action.card.isAccessCodeSet, action.card.isPasscodeSet ?: false
                        )
                        store.dispatch(DetailsAction.ManageSecurity.SetCurrentOption(simulatedResponse))
                        store.dispatch(DetailsAction.ManageSecurity.OpenSecurity)
                    } else {
                        scope.launch {
                            when (val response = tangemSdkManager.checkUserCodes(action.card.cardId)) {
                                is CompletionResult.Success -> {
                                    store.dispatchOnMain(DetailsAction.ManageSecurity.SetCurrentOption(response.data))
                                    store.dispatchOnMain(DetailsAction.ManageSecurity.OpenSecurity)
                                }
                                is CompletionResult.Failure -> {
                                }
                            }
                        }
                    }
                }
                is DetailsAction.ManageSecurity.OpenSecurity -> {
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.DetailsSecurity))
                }
                is DetailsAction.ManageSecurity.ConfirmSelection -> {
                    if (action.option != store.state.detailsState.securityScreenState?.currentOption) {
                        if (action.option == SecurityOption.LongTap) {
                            store.dispatch(DetailsAction.ManageSecurity.SaveChanges)
                        } else {
                            store.dispatch(NavigationAction.NavigateTo(AppScreen.DetailsConfirm))
                        }
                    } else {
                        store.dispatch(DetailsAction.ManageSecurity.ConfirmSelection.AlreadySet)
                    }
                }
                is DetailsAction.ManageSecurity.SaveChanges -> {
                    val cardId = store.state.detailsState.scanResponse?.card?.cardId
                    val selectedOption = store.state.detailsState.securityScreenState?.selectedOption
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
                                    if (selectedOption != SecurityOption.LongTap) {
                                        store.dispatch(NavigationAction.PopBackTo())
                                    }
                                    store.dispatch(DetailsAction.ManageSecurity.SaveChanges.Success)
                                }
                                is CompletionResult.Failure -> {
                                    (result.error as? TangemSdkError)?.let { error ->
                                        store.state.globalState.analyticsHandlers?.logCardSdkError(
                                            error = error,
                                            actionToLog = Analytics.ActionToLog.ChangeSecOptions,
                                            parameters = mapOf(
                                                AnalyticsParam.NEW_SECURITY_OPTION to
                                                    (selectedOption?.name ?: "")
                                            ),
                                            card = store.state.detailsState.scanResponse?.card
                                        )
                                    }
                                    store.dispatch(DetailsAction.ManageSecurity.SaveChanges.Failure)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

