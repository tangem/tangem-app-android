package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.tap.backupService
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import org.rekotlin.Action

object OnboardingWalletReducer {
    fun reduce(action: Action, state: AppState): OnboardingWalletState = internalReduce(action, state)
}

private fun internalReduce(action: Action, appState: AppState): OnboardingWalletState {
    val state = appState.onboardingWalletState

    return when (action) {
        is GlobalAction.Onboarding -> ReducerForGlobalAction.reduce(action, state)
        is BackupAction -> state.copy(backupState = BackupReducer.reduce(action, state.backupState))
        is OnboardingWallet2Action -> OnboardingWallet2Reducer.reduce(action, state)
        is OnboardingWalletAction.GetToCreateWalletStep -> state.copy(
            step = OnboardingWalletStep.CreateWallet,
        )
        is OnboardingWalletAction.ResumeBackup -> state.copy(step = OnboardingWalletStep.Backup)
        is OnboardingWalletAction.SetArtworkUrl -> {
            state.copy(cardArtworkUri = action.artworkUri)
        }
        is OnboardingWalletAction.Done -> state.copy(step = OnboardingWalletStep.Done)
        else -> state
    }
}

private object ReducerForGlobalAction {

    private const val MAX_BACKUP_CARDS = 2

    fun reduce(action: GlobalAction.Onboarding, state: OnboardingWalletState): OnboardingWalletState {
        return when (action) {
            is GlobalAction.Onboarding.Start -> {
                OnboardingWalletState(
                    backupState = state.backupState.copy(
                        maxBackupCards = MAX_BACKUP_CARDS,
                        canSkipBackup = action.canSkipBackup,
                    ),
                    isRingOnboarding = action.scanResponse.cardTypesResolver.isRing(),
                )
            }
            is GlobalAction.Onboarding.StartForUnfinishedBackup -> {
                OnboardingWalletState(
                    backupState = state.backupState.copy(
                        maxBackupCards = action.addedBackupCardsCount,
                        canSkipBackup = false,
                        isInterruptedBackup = true,
                    ),
                )
            }
            else -> state
        }
    }
}

private object OnboardingWallet2Reducer {
    fun reduce(action: OnboardingWallet2Action, state: OnboardingWalletState): OnboardingWalletState = when (action) {
        is OnboardingWallet2Action.SetDependencies -> state.copy(
            wallet2State = OnboardingWallet2State(
                maxProgress = action.maxProgress,
            ),
        )
        else -> state
    }
}

private object BackupReducer {
    @Suppress("ComplexMethod")
    fun reduce(action: BackupAction, state: BackupState): BackupState {
        return when (action) {
            is BackupAction.IntroduceBackup -> BackupState(
                backupStep = BackupStep.InitBackup,
                maxBackupCards = 2,
                canSkipBackup = state.canSkipBackup,
            )
            BackupAction.StartAddingPrimaryCard -> state.copy(backupStep = BackupStep.ScanOriginCard)
            BackupAction.StartAddingBackupCards -> state.copy(backupStep = BackupStep.AddBackupCards)
            BackupAction.AddBackupCard.Success -> state.copy(backupCardsNumber = state.backupCardsNumber + 1)
            is BackupAction.AddBackupCard.ChangeButtonLoading -> state.copy(showBtnLoading = action.isLoading)
            BackupAction.ShowAccessCodeInfoScreen -> state.copy(backupStep = BackupStep.SetAccessCode)
            BackupAction.ShowEnterAccessCodeScreen -> state.copy(
                backupStep = BackupStep.EnterAccessCode,
                accessCodeError = null,
            )
            is BackupAction.SaveFirstAccessCode -> state.copy(
                backupStep = BackupStep.ReenterAccessCode,
                accessCode = action.accessCode,
            )
            is BackupAction.SetAccessCodeError -> state.copy(accessCodeError = action.error)
            is BackupAction.PrepareToWritePrimaryCard -> if (state.primaryCardId == null) {
                state.copy(
                    primaryCardId = backupService.primaryCardId,
                    backupCardIds = backupService.backupCardIds,
                    backupCardsNumber = backupService.backupCardIds.size,
                    backupStep = BackupStep.WritePrimaryCard,
                )
            } else {
                state.copy(backupStep = BackupStep.WritePrimaryCard)
            }
            is BackupAction.PrepareToWriteBackupCard -> if (state.primaryCardId == null) {
                state.copy(
                    primaryCardId = backupService.primaryCardId,
                    backupCardIds = backupService.backupCardIds,
                    backupCardsNumber = backupService.backupCardIds.size,
                    backupStep = BackupStep.WriteBackupCard(action.cardNumber),
                )
            } else {
                state.copy(backupStep = BackupStep.WriteBackupCard(action.cardNumber))
            }
            is BackupAction.ErrorInBackupCard -> state.copy(hasBackupError = true)
            is BackupAction.SkipBackup -> state.copy(backupStep = BackupStep.Finished)
            is BackupAction.FinishBackup -> state.copy(backupStep = BackupStep.Finished)
            BackupAction.OnAccessCodeDialogClosed -> state.copy(backupStep = BackupStep.AddBackupCards)
            BackupAction.DiscardBackup -> BackupState()
            else -> state
        }
    }
}