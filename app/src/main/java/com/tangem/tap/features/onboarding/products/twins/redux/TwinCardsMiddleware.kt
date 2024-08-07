package com.tangem.tap.features.onboarding.products.twins.redux

import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.guard
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.utils.popTo
import com.tangem.core.analytics.Analytics

import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.common.extensions.makePrimaryWalletManager
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.common.util.twinsIsTwinned
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.entities.ProgressState
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.domain.twins.TwinCardsManager
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.features.onboarding.OnboardingDialog
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.mainScope
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware
import kotlin.reflect.KClass

object TwinCardsMiddleware {
    val handler = twinsWalletMiddleware
}

private val twinsWalletMiddleware: Middleware<AppState> = { dispatch, _ ->
    { next ->
        { action ->
            handle(action, dispatch)
            next(action)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod", "MagicNumber")
private fun handle(action: Action, dispatch: DispatchFunction) {
    if (action !is TwinCardsAction) return

    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingState.onboardingManager
    val twinCardsState = store.state.twinCardsState
    val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)

    fun getScanResponse(): ScanResponse {
        return when (twinCardsState.mode) {
            is CreateTwinWalletMode.CreateWallet -> onboardingManager?.scanResponse
            is CreateTwinWalletMode.RecreateWallet -> globalState.scanResponse
        } ?: throw NullPointerException("ScanResponse can't be NULL")
    }

    fun updateScanResponse(response: ScanResponse) {
        when (twinCardsState.mode) {
            is CreateTwinWalletMode.CreateWallet -> onboardingManager?.scanResponse = response
            is CreateTwinWalletMode.RecreateWallet -> store.dispatchOnMain(GlobalAction.SaveScanResponse(response))
        }
    }

    fun startCardActivation(cardId: String) {
        if (twinCardsState.mode == CreateTwinWalletMode.CreateWallet) {
            scope.launch {
                onboardingManager?.startActivation(cardId)
            }
        }
    }

    fun finishCardActivation() {
        if (twinCardsState.mode == CreateTwinWalletMode.CreateWallet) {
            Analytics.send(Onboarding.Finished())

            val cardIds = listOfNotNull(getScanResponse().card.cardId, twinCardsState.pairCardId)

            if (cardIds.isNotEmpty()) {
                mainScope.launch {
                    onboardingManager?.finishActivation(cardIds)
                }
            }
        }
    }

    when (action) {
        is TwinCardsAction.Init -> {
            mainScope.launch {
                if (twinCardsState.currentStep is TwinCardsStep.WelcomeOnly) return@launch

                if (twinCardsState.mode is CreateTwinWalletMode.RecreateWallet) {
                    store.dispatch(GlobalAction.SaveScanResponse(twinCardsState.mode.scanResponse))
                }

                val scanResponse = getScanResponse()
                onboardingManager?.apply {
                    if (!isActivationStarted(scanResponse.card.cardId)) {
                        Analytics.send(Onboarding.Started())
                    }
                }

                when (twinCardsState.mode) {
                    is CreateTwinWalletMode.CreateWallet -> {
                        mainScope.launch {
                            val wasTwinsOnboardingShown = store.inject(DaggerGraphState::wasTwinsOnboardingShownUseCase)
                                .invokeSync()

                            val dispatchAction = if (wasTwinsOnboardingShown) {
                                val step = when {
                                    !scanResponse.twinsIsTwinned() -> TwinCardsStep.CreateFirstWallet
                                    twinCardsState.walletBalance.balanceIsToppedUp() -> TwinCardsStep.Done
                                    else -> TwinCardsStep.TopUpWallet
                                }
                                TwinCardsAction.SetStepOfScreen(step)
                            } else {
                                TwinCardsAction.SetStepOfScreen(TwinCardsStep.Welcome)
                            }

                            store.dispatch(dispatchAction)
                        }
                    }
                    is CreateTwinWalletMode.RecreateWallet -> {
                        store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.Warning))
                    }
                }
            }
        }
        is TwinCardsAction.SetStepOfScreen -> {
            when (action.step) {
                is TwinCardsStep.WelcomeOnly, TwinCardsStep.Welcome -> {
                    Analytics.send(Onboarding.Twins.ScreenOpened())

                    scope.launch {
                        store.inject(DaggerGraphState::saveTwinsOnboardingShownUseCase).invoke()
                    }
                }
                is TwinCardsStep.CreateFirstWallet -> {
                    Analytics.send(Onboarding.CreateWallet.ScreenOpened())
                    Analytics.send(Onboarding.Twins.SetupStarted())
                }
                TwinCardsStep.TopUpWallet -> {
                    Analytics.send(Onboarding.Topup.ScreenOpened())
                    store.dispatch(TwinCardsAction.Balance.Update)
                }
                TwinCardsStep.Done -> {
                    finishCardActivation()
                    postUi(500) { store.dispatch(TwinCardsAction.Confetti.Show) }
                }
                TwinCardsStep.None,
                TwinCardsStep.Warning,
                TwinCardsStep.CreateSecondWallet,
                TwinCardsStep.CreateThirdWallet,
                -> Unit
            }
        }
        is TwinCardsAction.Wallet.LaunchFirstStep -> {
            val manager = TwinCardsManager(card = getScanResponse().card)
            store.dispatch(TwinCardsAction.CardsManager.Set(manager))

            scope.launch {
                when (val result = manager.createFirstWallet(action.initialMessage)) {
                    is CompletionResult.Success -> {
                        // remove wallet only after first step of retwin
                        scope.launch {
                            userWalletsListManager.delete(
                                listOfNotNull(UserWalletIdBuilder.scanResponse(getScanResponse()).build()),
                            )
                        }
                        Analytics.send(Onboarding.CreateWallet.WalletCreatedSuccessfully())
                        startCardActivation(result.data.cardId)
                        delay(DELAY_SDK_DIALOG_CLOSE)
                        withMainContext {
                            store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.CreateSecondWallet))
                        }
                    }
                    is CompletionResult.Failure -> {
                    }
                }
            }
        }
        is TwinCardsAction.Wallet.LaunchSecondStep -> {
            val manager = twinCardsState.twinCardsManager ?: return

            scope.launch {
                val result = manager.createSecondWallet(
                    action.initialMessage,
                    action.preparingMessage,
                    action.creatingWalletMessage,
                )
                when (result) {
                    is CompletionResult.Success -> {
                        startCardActivation(result.data.cardId)
                        delay(DELAY_SDK_DIALOG_CLOSE)
                        withMainContext {
                            store.dispatch(TwinCardsAction.SetPairCardId(result.data.cardId))
                            store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.CreateThirdWallet))
                        }
                    }
                    is CompletionResult.Failure -> {
                    }
                }
            }
        }
        is TwinCardsAction.Wallet.LaunchThirdStep -> {
            val manager = twinCardsState.twinCardsManager ?: return

            scope.launch {
                when (val result = manager.complete(action.message)) {
                    is Result.Success -> {
                        Analytics.send(Onboarding.Twins.SetupFinished())
                        updateScanResponse(result.data)

                        delay(DELAY_SDK_DIALOG_CLOSE)
                        withMainContext {
                            when (twinCardsState.mode) {
                                is CreateTwinWalletMode.CreateWallet -> {
                                    store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.TopUpWallet))
                                }
                                is CreateTwinWalletMode.RecreateWallet -> {
                                    store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.Done))
                                }
                            }
                        }
                    }
                    is Result.Failure -> {
                    }
                }
            }
        }
        is TwinCardsAction.Balance.Update -> {
            val walletManager = if (twinCardsState.walletManager != null) {
                twinCardsState.walletManager
            } else {
                val wmFactory = runBlocking {
                    store.inject(DaggerGraphState::blockchainSDKFactory).getWalletManagerFactorySync()
                }
                val walletManager = wmFactory?.makePrimaryWalletManager(getScanResponse()).guard {
                    val message = "Loading cancelled. Cause: wallet manager didn't created"
                    val customError = TapError.CustomError(message)
                    store.dispatchErrorNotification(customError)
                    return
                }
                dispatch(TwinCardsAction.SetWalletManager(walletManager))
                walletManager
            }
            val isLoadedBefore = twinCardsState.walletBalance.state != ProgressState.Loading
            val balanceIsLoading = twinCardsState.walletBalance.copy(
                currency = Currency.Blockchain(
                    walletManager.wallet.blockchain,
                    walletManager.wallet.publicKey.derivationPath?.rawPath,
                ),
                state = ProgressState.Loading,
                error = null,
                criticalError = null,
            )
            store.dispatch(TwinCardsAction.Balance.Set(balanceIsLoading))

            scope.launch {
                val loadedBalance = onboardingManager?.updateBalance(walletManager) ?: return@launch
                loadedBalance.criticalError?.let(store::dispatchErrorNotification)
                delay(if (isLoadedBefore) 0 else 300)
                withMainContext {
                    store.dispatch(TwinCardsAction.Balance.Set(loadedBalance))
                    store.dispatch(TwinCardsAction.Balance.SetCriticalError(loadedBalance.criticalError))
                    store.dispatch(TwinCardsAction.Balance.SetNonCriticalError(loadedBalance.error))
                }
            }
        }
        is TwinCardsAction.Balance.Set -> {
            if (action.balance.balanceIsToppedUp()) {
                OnboardingHelper.sendToppedUpEvent(getScanResponse())
                store.dispatchOnMain(TwinCardsAction.SetStepOfScreen(TwinCardsStep.Done))
            }
        }
        is TwinCardsAction.ShowAddressInfoDialog -> {
            val addressData = twinCardsState.walletManager?.getAddressData() ?: return

            val appDialog = AppDialog.AddressInfoDialog(twinCardsState.walletBalance.currency, addressData)
            store.dispatchDialogShow(appDialog)
        }
        is TwinCardsAction.TopUp -> {
            val walletManager = twinCardsState.walletManager.guard {
                store.dispatchDebugErrorNotification("NPE: WalletManager")
                return
            }

            val scanResponse = onboardingManager?.scanResponse ?: return
            val blockchain = walletManager.wallet.blockchain
            val cryptoCurrency = CryptoCurrencyFactory().createCoin(
                blockchain,
                null,
                scanResponse.derivationStyleProvider,
            ) ?: return
            val topUpUrl = walletManager.getTopUpUrl(cryptoCurrency) ?: return

            val currencyType = AnalyticsParam.CurrencyType.Blockchain(blockchain)
            Analytics.send(Onboarding.Topup.ButtonBuyCrypto(currencyType))

            if (globalState.userCountryCode == RUSSIA_COUNTRY_CODE) {
                val dialogData = AppDialog.RussianCardholdersWarningDialog.Data(topUpUrl)
                store.dispatchDialogShow(AppDialog.RussianCardholdersWarningDialog(dialogData))
            } else {
                store.dispatchOpenUrl(topUpUrl)
            }
        }
        TwinCardsAction.Done -> {
            val scanResponse = getScanResponse()
            when (twinCardsState.mode) {
                is CreateTwinWalletMode.CreateWallet -> {
                    store.dispatchOnMain(GlobalAction.Onboarding.Stop)
                    OnboardingHelper.trySaveWalletAndNavigateToWalletScreen(scanResponse)
                }
                is CreateTwinWalletMode.RecreateWallet -> {
                    scope.launch {
                        val walletsRepository = store.inject(DaggerGraphState::walletsRepository)

                        if (walletsRepository.shouldSaveUserWalletsSync()) {
                            OnboardingHelper.trySaveWalletAndNavigateToWalletScreen(scanResponse)
                        } else {
                            store.dispatchNavigationAction { popTo<AppRoute.Home>() }
                        }
                    }
                }
            }
        }
        is TwinCardsAction.SaveScannedTwinCardAndNavigateToWallet -> {
            OnboardingHelper.trySaveWalletAndNavigateToWalletScreen(
                scanResponse = action.scanResponse,
            )
        }
        is TwinCardsAction.OnBackPressed -> {
            if (twinCardsState.twinningInProgress) {
                store.dispatchDialogShow(OnboardingDialog.TwinningProcessNotCompleted)
            } else {
                val onOkCallback = {
                    val shouldReturnCardBack = twinCardsState.mode == CreateTwinWalletMode.CreateWallet &&
                        twinCardsState.currentStep != TwinCardsStep.TopUpWallet &&
                        twinCardsState.currentStep != TwinCardsStep.Done

                    OnboardingHelper.onInterrupted()
                    store.dispatch(TwinCardsAction.CardsManager.Release)

                    action.shouldResetTwinCardsWidget(shouldReturnCardBack) {
                        store.dispatchNavigationAction { popTo(routeClass = getPopBackScreen()) }
                    }
                }
                store.dispatchDialogShow(OnboardingDialog.InterruptOnboarding(onOkCallback))
            }
        }
        else -> Unit
    }
}

private fun getPopBackScreen(): KClass<out AppRoute> {
    val userWalletsListManager = store.inject(DaggerGraphState::generalUserWalletsListManager)

    return if (userWalletsListManager.hasUserWallets) {
        val isLocked = runCatching { userWalletsListManager.asLockable()?.isLockedSync!! }.getOrElse { false }

        if (isLocked) {
            AppRoute.Welcome::class
        } else {
            AppRoute.Wallet::class
        }
    } else {
        AppRoute.Home::class
    }
}