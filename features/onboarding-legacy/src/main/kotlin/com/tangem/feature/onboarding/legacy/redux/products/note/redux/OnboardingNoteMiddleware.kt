package com.tangem.feature.onboarding.legacy.redux.products.note.redux

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.guard
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.extensions.makePrimaryWalletManager
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.feature.onboarding.legacy.analytics.Onboarding
import com.tangem.feature.onboarding.legacy.redux.DaggerGraphState
import com.tangem.feature.onboarding.legacy.redux.OnboardingGlobalAction
import com.tangem.feature.onboarding.legacy.redux.OnboardingReduxState
import com.tangem.feature.onboarding.legacy.redux.common.Currency
import com.tangem.feature.onboarding.legacy.redux.common.ProgressState
import com.tangem.feature.onboarding.legacy.redux.common.WalletAddressData
import com.tangem.feature.onboarding.legacy.redux.inject
import com.tangem.feature.onboarding.legacy.redux.store.dispatchLegacy
import com.tangem.feature.onboarding.legacy.redux.store.mainScope
import com.tangem.feature.onboarding.legacy.redux.store.scope
import com.tangem.feature.onboarding.legacy.redux.store.store
import com.tangem.sdk.api.TapError
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

internal object OnboardingNoteMiddleware {
    val handler = onboardingNoteMiddleware
}

private val onboardingNoteMiddleware: Middleware<OnboardingReduxState> = { dispatch, state ->
    { next ->
        { action ->
            handleNoteAction(state, action, dispatch)
            next(action)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod", "MagicNumber", "UnusedPrivateMember")
private fun handleNoteAction(appState: () -> OnboardingReduxState?, action: Action, dispatch: DispatchFunction) {
    if (action !is OnboardingNoteAction) return

    val bridges = store.inject(DaggerGraphState::bridges)

    /** bridged */
    // if (DemoHelper.tryHandle(appState, action)) return
    if (bridges.onboardingCommonBridge.tryHandleDemoCard(action)) return

    val onboardingReduxState = store.state
    val onboardingManager = onboardingReduxState.onboardingState.onboardingManager ?: return

    val scanResponse = onboardingManager.scanResponse
    val card = onboardingManager.scanResponse.card
    val noteState = store.state.onboardingNoteState

    when (action) {
        is OnboardingNoteAction.Init -> {
            scope.launch {
                if (!onboardingManager.isActivationStarted(card.cardId)) {
                    Analytics.send(Onboarding.Started())
                }
            }
        }
        is OnboardingNoteAction.LoadCardArtwork -> {
            scope.launch {
                val artworkUrl = onboardingManager.loadArtworkUrl()
                withMainContext { store.dispatch(OnboardingNoteAction.SetArtworkUrl(artworkUrl)) }
            }
        }
        is OnboardingNoteAction.DetermineStepOfScreen -> {
            val step = when {
                card.wallets.isEmpty() -> OnboardingNoteStep.CreateWallet
                noteState.walletBalance.balanceIsToppedUp() -> OnboardingNoteStep.Done
                else -> OnboardingNoteStep.TopUpWallet
            }
            store.dispatch(OnboardingNoteAction.SetStepOfScreen(step))
        }
        is OnboardingNoteAction.SetStepOfScreen -> {
            when (action.step) {
                OnboardingNoteStep.CreateWallet -> {
                    Analytics.send(Onboarding.CreateWallet.ScreenOpened())
                }
                OnboardingNoteStep.TopUpWallet -> {
                    Analytics.send(Onboarding.Topup.ScreenOpened())
                    store.dispatch(OnboardingNoteAction.Balance.Update)
                }
                OnboardingNoteStep.Done -> {
                    Analytics.send(Onboarding.Finished())
                    mainScope.launch {
                        onboardingManager.finishActivation(card.cardId)
                        /** changed */
                        // postUi(DELAY_SDK_DIALOG_CLOSE) { store.dispatch(OnboardingNoteAction.Confetti.Show) }
                        delay(DELAY_SDK_DIALOG_CLOSE)
                        store.dispatch(OnboardingNoteAction.Confetti.Show)
                    }
                }
                else -> Unit
            }
        }
        is OnboardingNoteAction.CreateWallet -> {
            scope.launch {
                val result = store.inject(DaggerGraphState::tangemSdkManager).createProductWallet(scanResponse)
                with(Dispatchers.Main) {
                    when (result) {
                        is CompletionResult.Success -> {
                            Analytics.send(Onboarding.CreateWallet.WalletCreatedSuccessfully())
                            val updatedResponse = scanResponse.copy(card = result.data.card)
                            onboardingManager.scanResponse = updatedResponse
                            onboardingManager.startActivation(updatedResponse.card.cardId)
                            store.dispatch(OnboardingNoteAction.SetStepOfScreen(OnboardingNoteStep.TopUpWallet))
                        }
                        is CompletionResult.Failure -> Unit
                    }
                }
            }
        }
        is OnboardingNoteAction.Balance.Update -> {
            val walletManager = if (noteState.walletManager != null) {
                noteState.walletManager
            } else {
                val wmFactory = runBlocking {
                    store.inject(DaggerGraphState::blockchainSDKFactory).getWalletManagerFactorySync()
                }
                val walletManager = wmFactory?.makePrimaryWalletManager(scanResponse).guard {
                    val message = "Loading cancelled. Cause: wallet manager didn't created"

                    /** changed */
                    // val customError = TapError.CustomError(message)
                    // store.dispatchErrorNotification(customError)
                    bridges.notificationsBridge.dispatchErrorNotification(message)

                    return
                }
                dispatch(OnboardingNoteAction.SetWalletManager(walletManager))
                walletManager
            }

            val isLoadedBefore = noteState.walletBalance.state != ProgressState.Loading
            val balanceIsLoading = noteState.walletBalance.copy(
                currency = Currency.Blockchain(
                    walletManager.wallet.blockchain,
                    walletManager.wallet.publicKey.derivationPath?.rawPath,
                ),
                state = ProgressState.Loading,
                error = null,
                criticalError = null,
            )
            store.dispatch(OnboardingNoteAction.Balance.Set(balanceIsLoading))

            scope.launch {
                val loadedBalance = onboardingManager.updateBalance(walletManager)
                delay(if (isLoadedBefore) 0 else 300)
                /** changed */
                // loadedBalance.criticalError?.let { store.dispatchErrorNotification(it) }
                loadedBalance.criticalError?.let {
                    bridges.notificationsBridge.dispatchErrorNotification(it)
                }
                withMainContext {
                    store.dispatch(OnboardingNoteAction.Balance.Set(loadedBalance))
                    store.dispatch(OnboardingNoteAction.Balance.SetCriticalError(loadedBalance.criticalError))
                    store.dispatch(OnboardingNoteAction.Balance.SetNonCriticalError(loadedBalance.error))
                }
            }
        }
        is OnboardingNoteAction.Balance.Set -> {
            if (action.balance.balanceIsToppedUp()) {
                /** changed */
                // OnboardingHelper.sendToppedUpEvent(scanResponse)
                bridges.onboardingCommonBridge.sendToppedUpAnalyticEvent(scanResponse)
                store.dispatch(OnboardingNoteAction.SetStepOfScreen(OnboardingNoteStep.Done))
            }
        }
        is OnboardingNoteAction.ShowAddressInfoDialog -> {
            val addressData = noteState.walletManager?.getAddressData() ?: return

            Analytics.send(Onboarding.Topup.ButtonShowWalletAddress())
            /** changed */
            // val appDialog = Dialog.AddressInfoDialog(noteState.walletBalance.currency, addressData)
            // store.dispatchDialogShow(appDialog)

            // TODO: implement
            // val appDialog =
            //     NotificationsBridge.Dialog.AddressInfoDialog(noteState.walletBalance.currency, addressData)
            // bridges.notificationsBridge.dispatchDialogShow(appDialog)
        }
        is OnboardingNoteAction.TopUp -> {
            val walletManager = noteState.walletManager.guard {
                /** changed */
                // store.dispatchDebugErrorNotification("NPE: WalletManager")
                bridges.notificationsBridge.dispatchErrorNotification(
                    TapError.CustomError("DEBUG ERROR: NPE: WalletManager"),
                )
                return
            }

            /** changed */
            // OnboardingHelper.handleTopUpAction(
            //     walletManager = walletManager,
            //     scanResponse = scanResponse,
            //     globalState = onboardingReduxState,
            // )
            bridges.onboardingCommonBridge.handleTopUpAction(
                walletManager = walletManager,
                scanResponse = scanResponse,
            )
        }
        is OnboardingNoteAction.Done -> {
            /* changed */
            // store.dispatch(GlobalAction.Onboarding.Stop)
            store.dispatchLegacy(OnboardingGlobalAction.Stop)
            /* changed */
            // OnboardingHelper.trySaveWalletAndNavigateToWalletScreen(scanResponse)
            bridges.onboardingCommonBridge.trySaveWalletAndNavigateToWalletScreen(scanResponse)
        }
        is OnboardingNoteAction.OnBackPressed -> {
            // TODO: implement
            // store.dispatchDialogShow(
            //     OnboardingDialog.InterruptOnboarding(
            //         onOk = {
            //             OnboardingHelper.onInterrupted()
            //             store.dispatchNavigationAction(AppRouter::pop)
            //         },
            //     ),
            // )
        }
        else -> Unit
    }
}

private fun WalletManager?.getAddressData(): WalletAddressData? {
    val wallet = this?.wallet ?: return null

    val addressDataList = wallet.createAddressesData()
    return if (addressDataList.isEmpty()) null else addressDataList[0]
}

private fun Wallet.createAddressesData(): List<WalletAddressData> {
    val listOfAddressData = mutableListOf<WalletAddressData>()
    // put a defaultAddress at the first place
    addresses.forEach {
        val addressData = WalletAddressData(
            it.value,
            it.type,
            getShareUri(it.value),
            getExploreUrl(it.value),
        )
        if (it.type == AddressType.Default) {
            listOfAddressData.add(0, addressData)
        } else {
            listOfAddressData.add(addressData)
        }
    }
    return listOfAddressData
}
