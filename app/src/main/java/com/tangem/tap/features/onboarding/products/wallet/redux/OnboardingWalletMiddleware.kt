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
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeMiddleware
import com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteAction
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

class OnboardingWalletMiddleware {
    companion object {
        val handler = onboardingWalletMiddleware
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
    val onboardingManager = globalState.onboardingManager

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
                            val updatedResponse = scanResponse.copy(card = result.data)
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

            (listOf(walletState.backupState.primaryCardId) +
                    walletState.backupState.backupCardIds + scanResponse?.card?.cardId)
                .distinct().filterNotNull()
                .forEach { cardId ->
                    preferencesStorage.usedCardsPrefStorage.activationFinished(cardId)
                }

            if (scanResponse == null) {
                store.dispatch(NavigationAction.PopBackTo())
                store.dispatch(HomeAction.ReadCard)
            } else {
                scope.launch { globalState.tapWalletManager.onCardScanned(scanResponse) }
                store.dispatchOnMain(NavigationAction.NavigateTo(AppScreen.Wallet))
            }
        }
        OnboardingWalletAction.ProceedBackup -> {
            val newAction = when (val backupState = backupService.currentState) {
                BackupService.State.Preparing -> BackupAction.IntroduceBackup
                BackupService.State.FinalizingPrimaryCard -> BackupAction.PrepareToWritePrimaryCard
                is BackupService.State.FinalizingBackupCard ->
                    BackupAction.PrepareToWriteBackupCard(backupState.index)
                BackupService.State.Finished -> BackupAction.FinishBackup
            }
            store.dispatch(newAction)
        }
        OnboardingWalletAction.OnBackPressed -> {
            when (walletState.backupState.backupStep) {
                BackupStep.InitBackup, BackupStep.Finished -> store.dispatch(NavigationAction.PopBackTo())
                BackupStep.ScanOriginCard, BackupStep.AddBackupCards, BackupStep.EnterAccessCode,
                BackupStep.ReenterAccessCode, BackupStep.SetAccessCode,
                -> {
                    store.dispatch(BackupAction.DiscardBackup)
                    store.dispatch(NavigationAction.PopBackTo())
                }
                is BackupStep.WriteBackupCard, BackupStep.WritePrimaryCard ->
                    store.dispatch(GlobalAction.ShowDialog(BackupDialog.BackupInProgress))


            }
        }
    }
}

class BackupMiddleware {
    val backupMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                val backupState = state()?.onboardingWalletState?.backupState
                when (action) {
                    is BackupAction.StartBackup -> {
                        backupService.discardSavedBackup()
                    }
                    is BackupAction.ScanPrimaryCard -> {
                        backupService.readPrimaryCard { result ->
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
                        store.dispatchOpenUrl(HomeMiddleware.CARD_SHOP_URI)
                    }
                    is BackupAction.FinishAddingBackupCards -> {
                        if (backupService.addedBackupCardsCount == 1) {
                            store.dispatchOnMain(GlobalAction.ShowDialog(BackupDialog.BuyMoreBackupCards))
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
                        if (action.accessCodeConfirmation == backupState?.accessCode) {
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
//                        store.dispatch(OnboardingWalletAction.Done)
                    }
                    is BackupAction.DiscardBackup -> {
                        backupService.discardSavedBackup()
                    }
                    is BackupAction.CheckForUnfinishedBackup -> {
                        if (backupService.hasIncompletedBackup) {
                            store.dispatch(GlobalAction.ShowDialog(BackupDialog.UnfinishedBackupFound))
                        }
                    }
                    is BackupAction.ResumeBackup -> {
                        store.dispatch(OnboardingWalletAction.ProceedBackup)
                        store.dispatch(NavigationAction.NavigateTo(AppScreen.OnboardingWallet))
                    }
                    is BackupAction.DismissBackup -> {
                        store.dispatch(BackupAction.FinishBackup)
                    }
                }

                next(action)
            }
        }
    }
}