package com.tangem.tap.features.onboarding.products.otherCards.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.features.wallet.models.toCurrencies
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

object OnboardingOtherCardsMiddleware {
    val handler = onboardingOtherCardsMiddleware
}

private val onboardingOtherCardsMiddleware: Middleware<AppState> = { dispatch, _ ->
    { next ->
        { action ->
            handleOtherCardsAction(action)
            next(action)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod", "MagicNumber")
private fun handleOtherCardsAction(action: Action) {
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
                card.wallets.isEmpty() -> OnboardingOtherCardsStep.CreateWallet
                else -> OnboardingOtherCardsStep.Done
            }
            store.dispatch(OnboardingOtherCardsAction.SetStepOfScreen(step))
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
                else -> Unit
            }
        }
        is OnboardingOtherCardsAction.CreateWallet -> {
            scope.launch {
                val result = tangemSdkManager.createProductWallet(onboardingManager.scanResponse)
                withMainContext {
                    when (result) {
                        is CompletionResult.Success -> {
                            Analytics.send(Onboarding.CreateWallet.WalletCreatedSuccessfully())
                            val updatedResponse = onboardingManager.scanResponse.copy(
                                card = result.data.card,
                            )
                            val updatedCard = updatedResponse.card
                            onboardingManager.scanResponse = updatedResponse
                            onboardingManager.activationStarted(updatedCard.cardId)
                            store.state.globalState.topUpController?.registerEmptyWallet(updatedResponse)

                            val primaryBlockchain = updatedResponse.cardTypesResolver.getBlockchain()
                            val blockchainNetworks = if (primaryBlockchain != Blockchain.Unknown) {
                                val primaryToken = updatedResponse.cardTypesResolver.getPrimaryToken()
                                val blockchainNetwork =
                                    BlockchainNetwork(
                                        blockchain = primaryBlockchain,
                                        card = updatedCard,
                                    )
                                        .updateTokens(
                                            listOfNotNull(primaryToken),
                                        )
                                listOf(blockchainNetwork)
                            } else {
                                listOf(
                                    BlockchainNetwork(
                                        blockchain = Blockchain.Bitcoin,
                                        card = updatedCard,
                                    ),
                                    BlockchainNetwork(
                                        blockchain = Blockchain.Ethereum,
                                        card = updatedCard,
                                    ),
                                )
                            }

                            scope.launch {
                                userTokensRepository.saveUserTokens(
                                    card = result.data.card,
                                    tokens = blockchainNetworks.toCurrencies(),
                                )
                            }

                            delay(DELAY_SDK_DIALOG_CLOSE)
                            store.dispatch(OnboardingOtherCardsAction.SetStepOfScreen(OnboardingOtherCardsStep.Done))
                        }
                        is CompletionResult.Failure -> Unit
                    }
                }
            }
        }
        OnboardingOtherCardsAction.Done -> {
            store.dispatch(GlobalAction.Onboarding.Stop)
            OnboardingHelper.trySaveWalletAndNavigateToWalletScreen(onboardingManager.scanResponse)
        }
        OnboardingOtherCardsAction.OnBackPressed -> {
            OnboardingHelper.onInterrupted()
        }
        else -> Unit
    }
}