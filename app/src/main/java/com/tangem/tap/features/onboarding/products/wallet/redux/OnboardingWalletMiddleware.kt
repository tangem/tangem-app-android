package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CardFilter
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.extensions.ifNotNull
import com.tangem.domain.common.SaltPayWorkaround
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.withMainContext
import com.tangem.operations.backup.BackupService
import com.tangem.tap.backupService
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.extensions.hasWallets
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayAction
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayState
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

class OnboardingWalletMiddleware {
    companion object {
        val handler = onboardingWalletMiddleware
    }
}

private val onboardingWalletMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleWalletAction(action, state, dispatch)
            next(action)
        }
    }
}

private fun handleWalletAction(action: Action, state: () -> AppState?, dispatch: DispatchFunction) {
    if (action !is OnboardingWalletAction) return

    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingState.onboardingManager

    val scanResponse = onboardingManager?.scanResponse
    val card = scanResponse?.card

    val onboardingWalletState = store.state.onboardingWalletState

    when (action) {
        OnboardingWalletAction.Init -> {
            // if (walletState.onboardingSaltPayState?.isTest == true) {
            // store.dispatch(BackupAction.FinishBackup)
            // return
            // }

            when {
                card == null -> {
                    // это возможно только когда обнуражен незаконченный бэкап
                    // StartForUnfinishedBackup -> navigateTo(OnboardingWallet) -> OnboardingWalletAction.Init
                    store.dispatch(OnboardingWalletAction.ResumeBackup)
                }
                card.hasWallets() && card.backupStatus == Card.BackupStatus.NoBackup -> {
                    store.dispatch(OnboardingWalletAction.ResumeBackup)
                }
                card.hasWallets() && card.backupStatus?.isActive == true -> {
                    if (scanResponse.isSaltPay()) {
                        store.dispatch(OnboardingWalletAction.GetToSaltPayStep)
                    }
                    store.dispatch(BackupAction.FinishBackup)
                }
                else -> {
                    store.dispatch(OnboardingWalletAction.GetToCreateWalletStep)
                }
            }
        }
        is OnboardingWalletAction.StartSaltPayCardActivation -> {
            // we need to do this because the standard activation happens in the OnboardingWalletAction.CreateWallet
            // step which is omitted for SaltPay cards
            if (onboardingWalletState.isSaltPay) {
                ifNotNull(onboardingManager, scanResponse?.card?.cardId) { manager, cardId ->
                    manager.activationStarted(cardId)
                }
            }
        }
        is OnboardingWalletAction.LoadArtwork -> {
            scope.launch {
                val artwork = onboardingManager?.loadArtworkUrl()
                val cardArtwork = if (artwork == Artwork.DEFAULT_IMG_URL) {
                    null
                } else {
                    artwork
                }
                store.dispatchOnMain(OnboardingWalletAction.SetArtworkUrl(cardArtwork))
            }
        }
        is OnboardingWalletAction.CreateWallet -> {
            scope.launch {
                scanResponse ?: return@launch
                val result = tangemSdkManager.createProductWallet(scanResponse)
                withMainContext {
                    when (result) {
                        is CompletionResult.Success -> {
                            //here we must use updated scanResponse after createWallet & derivation
                            val updatedResponse = globalState.onboardingState.onboardingManager.scanResponse.copy(
                                card = result.data.card,
                                derivedKeys = result.data.derivedKeys,
                                primaryCard = result.data.primaryCard,
                            )
                            onboardingManager.scanResponse = updatedResponse
                            val blockchainNetworks = listOf(
                                BlockchainNetwork(Blockchain.Bitcoin, result.data.card),
                                BlockchainNetwork(Blockchain.Ethereum, result.data.card),
                            )
                            store.dispatch(
                                WalletAction.MultiWallet.SaveCurrencies(
                                    blockchainNetworks = blockchainNetworks,
                                    card = result.data.card,
                                ),
                            )
                            onboardingManager.activationStarted(updatedResponse.card.cardId)
                            store.dispatch(OnboardingWalletAction.ResumeBackup)
                        }
                        is CompletionResult.Failure -> {
//                            do nothing
                        }
                    }
                }
            }
        }
        OnboardingWalletAction.FinishOnboarding -> {
            store.dispatch(GlobalAction.Onboarding.Stop)
// [REDACTED_TODO_COMMENT]
            if (scanResponse == null) {
                store.dispatch(NavigationAction.PopBackTo())
                store.dispatch(HomeAction.ReadCard)
            } else {
                val backupState = store.state.onboardingWalletState.backupState
                val updatedScanResponse = updateScanResponseAfterBackup(scanResponse, backupState)
                scope.launch { globalState.tapWalletManager.onCardScanned(updatedScanResponse) }
                store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
            }
        }
        is OnboardingWalletAction.ResumeBackup -> {
            val newAction = when (val backupState = backupService.currentState) {
                BackupService.State.FinalizingPrimaryCard -> BackupAction.PrepareToWritePrimaryCard
                is BackupService.State.FinalizingBackupCard -> BackupAction.PrepareToWriteBackupCard(backupState.index)
                else -> {
                    if (onboardingWalletState.backupState.backupStep == BackupStep.InitBackup ||
                        onboardingWalletState.backupState.backupStep == BackupStep.Finished
                    ) {
                        if (onboardingWalletState.isSaltPay) {
                            BackupAction.StartBackup
                        } else {
                            BackupAction.IntroduceBackup
                        }
                    } else {
                        null
                    }
                }
            }
            newAction?.let { store.dispatch(it) }
        }
        OnboardingWalletAction.OnBackPressed -> {
            when (onboardingWalletState.backupState.backupStep) {
                BackupStep.InitBackup, BackupStep.Finished -> store.dispatch(NavigationAction.PopBackTo())
                BackupStep.ScanOriginCard, BackupStep.AddBackupCards, BackupStep.EnterAccessCode,
                BackupStep.ReenterAccessCode, BackupStep.SetAccessCode, BackupStep.WritePrimaryCard,
                -> {
                    store.dispatch(BackupAction.DiscardBackup)
                    store.dispatch(NavigationAction.PopBackTo())
                }
                is BackupStep.WriteBackupCard ->
                    store.dispatch(GlobalAction.ShowDialog(BackupDialog.BackupInProgress))
            }
        }
    }
}

private fun updateScanResponseAfterBackup(
    scanResponse: ScanResponse, backupState: BackupState,
): ScanResponse {
    val card = if (backupState.backupCardsNumber > 0) {
        val cardsCount = backupState.backupCardsNumber
        scanResponse.card.copy(
            backupStatus = Card.BackupStatus.Active(cardCount = cardsCount),
            isAccessCodeSet = true,
        )
    } else {
        scanResponse.card
    }
    return scanResponse.copy(card = card)
}

class BackupMiddleware {
    val backupMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                if (action is BackupAction) handleBackupAction(state, action)
                next(action)
            }
        }
    }
}

private fun handleBackupAction(appState: () -> AppState?, action: BackupAction) {
    if (DemoHelper.tryHandle(appState, action)) return
    val globalState = appState()?.globalState ?: return
    val onboardingWalletState = appState()?.onboardingWalletState ?: return

    val backupState = onboardingWalletState.backupState
    val scanResponse = globalState.onboardingState.onboardingManager?.scanResponse
    val card = scanResponse?.card

    when (action) {
        is BackupAction.StartBackup -> {
            backupService.discardSavedBackup()
            val primaryCard = scanResponse?.primaryCard
            if (primaryCard != null) {
                backupService.setPrimaryCard(primaryCard)
                store.dispatch(BackupAction.StartAddingBackupCards)
            } else {
                store.dispatch(BackupAction.StartAddingPrimaryCard)
            }
        }
        is BackupAction.ScanPrimaryCard -> {
            backupService.readPrimaryCard(cardId = card?.cardId) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        store.dispatchOnMain(BackupAction.StartAddingBackupCards)
                    }
                    is CompletionResult.Failure -> {
                    }
                }
            }
        }
        is BackupAction.AddBackupCard -> {
            backupService.skipCompatibilityChecks = true
            tangemSdk.config.filter.cardIdFilter = CardFilter.Companion.CardIdFilter.Allow(
                items = SaltPayWorkaround.walletCardIds.toSet(),
                ranges = SaltPayWorkaround.walletCardIdRanges,
            )

            backupService.addBackupCard { result ->
                backupService.skipCompatibilityChecks = false
                tangemSdk.config.filter.cardIdFilter = null
                when (result) {
                    is CompletionResult.Success -> {
                        store.dispatchOnMain(BackupAction.AddBackupCard.Success)
                    }
                    is CompletionResult.Failure -> {
                    }
                }
            }
        }
        is BackupAction.FinishAddingBackupCards -> {
            if (backupService.addedBackupCardsCount == backupState.maxBackupCards) {
                store.dispatchOnMain(BackupAction.ShowAccessCodeInfoScreen)
            } else {
                store.dispatchOnMain(GlobalAction.ShowDialog(BackupDialog.AddMoreBackupCards))
            }
        }
        is BackupAction.CheckAccessCode -> {
            if (action.accessCode.length < 4) {
                store.dispatch(BackupAction.SetAccessCodeError(AccessCodeError.CodeTooShort))
            } else {
                store.dispatch(BackupAction.SetAccessCodeError(null))
                store.dispatch(BackupAction.SaveFirstAccessCode(action.accessCode))
            }
        }
        is BackupAction.SaveAccessCodeConfirmation -> {
            if (action.accessCodeConfirmation == backupState.accessCode) {
                store.dispatch(BackupAction.SetAccessCodeError(null))
                backupService.setAccessCode(action.accessCodeConfirmation)
                store.dispatch(BackupAction.PrepareToWritePrimaryCard)
            } else {
                store.dispatch(BackupAction.SetAccessCodeError(AccessCodeError.CodesDoNotMatch))
            }
        }
        is BackupAction.WritePrimaryCard -> {
            backupService.proceedBackup { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        store.dispatchOnMain(BackupAction.PrepareToWriteBackupCard(1))
                    }
                    is CompletionResult.Failure -> {
                    }
                }
            }
        }
        is BackupAction.WriteBackupCard -> {
            backupService.proceedBackup { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        if (backupService.currentState == BackupService.State.Finished) {
                            initSaltPayOnBackupFinishedIfNeeded(scanResponse, onboardingWalletState)
                            store.dispatchOnMain(BackupAction.FinishBackup)
                        } else {
                            store.dispatchOnMain(BackupAction.PrepareToWriteBackupCard(action.cardNumber + 1))
                        }
                    }
                    is CompletionResult.Failure -> {
                    }
                }
            }
        }
        is BackupAction.FinishBackup -> {
            (listOf(backupState.primaryCardId, card?.cardId) + backupState.backupCardIds)
                .distinct().filterNotNull()
                .forEach { cardId ->
                    preferencesStorage.usedCardsPrefStorage.activationFinished(cardId)
                }
        }
        is BackupAction.DiscardBackup -> {
            backupService.discardSavedBackup()
        }
        is BackupAction.DiscardSavedBackup -> {
            backupService.primaryCardId?.let {
                preferencesStorage.usedCardsPrefStorage.activationFinished(it)
            }
            backupService.discardSavedBackup()
        }
        is BackupAction.CheckForUnfinishedBackup -> {
            if (backupService.hasIncompletedBackup) {
                store.dispatch(GlobalAction.ShowDialog(BackupDialog.UnfinishedBackupFound))
            }
        }
        is BackupAction.ResumeFoundUnfinishedBackup -> {
            // val isSaltPay = backupService.primaryBatchId?.let { SaltPayWorkaround.isVisaBatchId(it) } ?: false
            // store.dispatch(GlobalAction.Onboarding.StartForUnfinishedBackup(isSaltPay))
            // store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
        }
        is BackupAction.DismissBackup -> {
            if (onboardingWalletState.isSaltPay) throw UnsupportedOperationException()
            store.dispatch(BackupAction.FinishBackup)
        }
    }
}

/**
 * SaltPay state maybe not initialized if backup resumed from ResumeFoundUnfinishedBackup action
 */
private fun initSaltPayOnBackupFinishedIfNeeded(
    scanResponse: ScanResponse?,
    onboardingWalletState: OnboardingWalletState,
) {
    if (onboardingWalletState.isSaltPay && onboardingWalletState.onboardingSaltPayState == null) {
// [REDACTED_TODO_COMMENT]
        if (scanResponse == null) throw IllegalArgumentException()

        val (manager, config) = OnboardingSaltPayState.initDependency(scanResponse)
        store.dispatchOnMain(OnboardingSaltPayAction.Init.SetDependencies(manager, config))
        store.dispatchOnMain(OnboardingSaltPayAction.Update)
    }
}
