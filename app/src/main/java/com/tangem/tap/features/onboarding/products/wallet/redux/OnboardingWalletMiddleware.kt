package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.operations.backup.BackupService
import com.tangem.tap.*
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.extensions.withMainContext
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.extensions.hasWallets
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteAction
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletMiddleware.Companion.BUY_WALLET_URL
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

class OnboardingWalletMiddleware {
    companion object {
        val handler = onboardingWalletMiddleware

        const val BUY_WALLET_URL = "https://wallet.tangem.com/"
    }
}

private val onboardingWalletMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleWalletAction(action)
            next(action)
        }
    }
}

private fun handleWalletAction(action: Action) {
    if (action !is OnboardingWalletAction) return
    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingState.onboardingManager

    val scanResponse = onboardingManager?.scanResponse
    val card = scanResponse?.card

    val walletState = store.state.onboardingWalletState

    when (action) {
        OnboardingWalletAction.Init -> {
            val action = when {
                card == null -> {
                    OnboardingWalletAction.ProceedBackup
                }
                card.hasWallets() && card.backupStatus == Card.BackupStatus.NoBackup ->
                    OnboardingWalletAction.ProceedBackup
                card.hasWallets() && card.backupStatus?.isActive == true ->
                    BackupAction.FinishBackup
                else -> OnboardingWalletAction.GetToCreateWalletStep
            }
            store.dispatch(action)
        }
        is OnboardingNoteAction.LoadCardArtwork -> {
//            scope.launch {
//                val artwork = onboardingManager.loadArtwork()
//                store.dispatchOnMain(OnboardingWalletAction.SetArtworkUrl(artwork))
//            }
        }
        is OnboardingWalletAction.CreateWallet -> {
            scope.launch {
                scanResponse ?: return@launch
                val result = tangemSdkManager.createProductWallet(scanResponse)
                withMainContext {
                    when (result) {
                        is CompletionResult.Success -> {
                            //here we must use updated scanResponse after createWallet & derivation
                            val updatedResponse =
                                globalState.onboardingState.onboardingManager.scanResponse.copy(
                                    card = result.data.card,
                                    derivedKeys = result.data.derivedKeys,
                                    primaryCard = result.data.primaryCard
                                )
                            onboardingManager.scanResponse = updatedResponse
                            onboardingManager.activationStarted(updatedResponse.card.cardId)
                            store.dispatch(OnboardingWalletAction.ProceedBackup)
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
        OnboardingWalletAction.ProceedBackup -> {
            val newAction = when (val backupState = backupService.currentState) {
                BackupService.State.FinalizingPrimaryCard -> BackupAction.PrepareToWritePrimaryCard
                is BackupService.State.FinalizingBackupCard ->
                    BackupAction.PrepareToWriteBackupCard(backupState.index)
                else -> BackupAction.IntroduceBackup
            }
            store.dispatch(newAction)
        }
        OnboardingWalletAction.OnBackPressed -> {
            when (walletState.backupState.backupStep) {
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
            isAccessCodeSet = true
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
                if (action is BackupAction) handleBackupAction(action)
                next(action)
            }
        }
    }
}

private fun handleBackupAction(action: BackupAction) {
    val backupState = store.state.onboardingWalletState.backupState

    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingState.onboardingManager

    val scanResponse = onboardingManager?.scanResponse
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
            backupService.addBackupCard { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        store.dispatchOnMain(BackupAction.AddBackupCard.Success)
                    }
                    is CompletionResult.Failure -> {

                    }
                }
            }
        }
        is BackupAction.GoToShop -> {
            store.dispatchOpenUrl(BUY_WALLET_URL)
        }
        is BackupAction.FinishAddingBackupCards -> {
            if (backupService.addedBackupCardsCount == 1) {
                store.dispatchOnMain(GlobalAction.ShowDialog(BackupDialog.AddMoreBackupCards))
            }
            if (backupService.addedBackupCardsCount == 2) {
                store.dispatchOnMain(BackupAction.ShowAccessCodeInfoScreen)
            }
        }
        is BackupAction.CheckAccessCode -> {
            if (action.accessCode.length < 4) {
                store.dispatch(BackupAction.SetAccessCodeError(
                    AccessCodeError.CodeTooShort
                ))
            } else {
                store.dispatch(BackupAction.SaveFirstAccessCode(action.accessCode))
            }
        }
        is BackupAction.SaveAccessCodeConfirmation -> {
            if (action.accessCodeConfirmation == backupState.accessCode) {
                backupService.setAccessCode(action.accessCodeConfirmation)
                store.dispatch(BackupAction.PrepareToWritePrimaryCard)
            } else {
                store.dispatch(BackupAction.SetAccessCodeError(
                    AccessCodeError.CodesDoNotMatch
                ))
            }
        }
        is BackupAction.WritePrimaryCard -> {
            backupService.proceedBackup { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        store.dispatchOnMain(
                            BackupAction.PrepareToWriteBackupCard(1)
                        )
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
                            store.dispatchOnMain(BackupAction.FinishBackup)
                        } else {
                            store.dispatchOnMain(
                                BackupAction.PrepareToWriteBackupCard(action.cardNumber + 1)
                            )
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
        is BackupAction.ResumeBackup -> {
            store.dispatch(GlobalAction.Onboarding.Start(null, true))
            store.dispatch(OnboardingWalletAction.ProceedBackup)
            store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
        }
        is BackupAction.DismissBackup -> {
            store.dispatch(BackupAction.FinishBackup)
        }
    }
}