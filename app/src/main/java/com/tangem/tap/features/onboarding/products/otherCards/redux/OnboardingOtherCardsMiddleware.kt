package com.tangem.tap.features.onboarding.products.otherCards.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.onCardScanned
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.extensions.hasWallets
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

class OnboardingOtherCardsMiddleware {
    companion object {
        val handler = onboardingOtherCardsMiddleware
    }
}

private val onboardingOtherCardsMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleOtherCardsAction(action, dispatch)
            next(action)
        }
    }
}

private fun handleOtherCardsAction(action: Action, dispatch: DispatchFunction) {
    if (action !is OnboardingOtherCardsAction) return
    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingState.onboardingManager ?: return

    val card = onboardingManager.scanResponse.card

    when (action) {
        is OnboardingOtherCardsAction.Init -> {
            if (!onboardingManager.isActivationStarted(card.cardId)) {
                Analytics.send(Onboarding.Started())
            }
        }
        is OnboardingOtherCardsAction.LoadCardArtwork -> {
            scope.launch {
                val artworkUrl = onboardingManager.loadArtworkUrl()
                withMainContext { store.dispatch(OnboardingOtherCardsAction.SetArtworkUrl(artworkUrl)) }
            }
        }
        is OnboardingOtherCardsAction.DetermineStepOfScreen -> {
            val step = when {
                !card.hasWallets() -> OnboardingOtherCardsStep.CreateWallet
                else -> OnboardingOtherCardsStep.Done
            }
            store.dispatch((OnboardingOtherCardsAction.SetStepOfScreen(step)))
        }
        is OnboardingOtherCardsAction.SetStepOfScreen -> {
            when (action.step) {
                OnboardingOtherCardsStep.CreateWallet -> {
                    Analytics.send(Onboarding.CreateWallet.ScreenOpened())
                }
                OnboardingOtherCardsStep.Done -> {
                    Analytics.send(Onboarding.Finished())
                    onboardingManager.activationFinished(card.cardId)
                    postUi(200) { store.dispatch(OnboardingOtherCardsAction.Confetti.Show) }
                }
            }
        }
        is OnboardingOtherCardsAction.CreateWallet -> {
            Analytics.send(Onboarding.CreateWallet.ButtonCreateWallet())
            scope.launch {
                val result = tangemSdkManager.createProductWallet(onboardingManager.scanResponse)
                withMainContext {
                    when (result) {
                        is CompletionResult.Success -> {
                            Analytics.send(Onboarding.CreateWallet.WalletCreatedSuccessfully())
                            val updatedResponse = onboardingManager.scanResponse.copy(
                                card = result.data.card,
                            )
                            onboardingManager.scanResponse = updatedResponse
                            onboardingManager.activationStarted(updatedResponse.card.cardId)

                            val primaryBlockchain = updatedResponse.getBlockchain()
                            val blockchainNetworks = if (primaryBlockchain != Blockchain.Unknown) {
                                val primaryToken = updatedResponse.getPrimaryToken()
                                val blockchainNetwork =
                                    BlockchainNetwork(primaryBlockchain, updatedResponse.card).updateTokens(
                                        listOfNotNull(primaryToken),
                                    )
                                listOf(blockchainNetwork)
                            } else {
                                listOf(
                                    BlockchainNetwork(Blockchain.Bitcoin, updatedResponse.card),
                                    BlockchainNetwork(Blockchain.Ethereum, updatedResponse.card),
                                )
                            }

                            store.dispatch(
                                WalletAction.MultiWallet.SaveCurrencies(blockchainNetworks, updatedResponse.card),
                            )

                            delay(DELAY_SDK_DIALOG_CLOSE)
                            store.dispatch(OnboardingOtherCardsAction.SetStepOfScreen(OnboardingOtherCardsStep.Done))
                        }
                        is CompletionResult.Failure -> {
//                            do nothing
                        }
                    }
                }
            }
        }
        OnboardingOtherCardsAction.Done -> {
            store.dispatch(GlobalAction.Onboarding.Stop)
            scope.launch {
                store.onCardScanned(onboardingManager.scanResponse)
                withMainContext { store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet)) }
            }
        }
    }
}
